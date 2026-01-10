package litejava.plugins.transaction;

import litejava.Plugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Seata 分布式事务插件 - AT 模式
 * 
 * 原理：
 * 1. 一阶段：业务 SQL 执行前记录 beforeImage，执行后记录 afterImage
 * 2. 二阶段提交：删除 undo_log
 * 3. 二阶段回滚：根据 undo_log 生成反向 SQL 执行
 * 
 * 配置：
 * seata:
 *   enabled: true
 *   serverAddr: localhost:8091
 *   txServiceGroup: my_tx_group
 */
public class SeataPlugin extends Plugin {
    
    public boolean enabled = true;
    public String serverAddr = "localhost:8091";
    public String applicationId;
    public String txServiceGroup = "default_tx_group";
    
    private static ThreadLocal<TxContext> currentTx = new ThreadLocal<>();
    private Map<String, GlobalTransaction> globalTransactions = new ConcurrentHashMap<>();
    
    @Override
    public void config() {
        enabled = app.conf.getBool("seata", "enabled", true);
        serverAddr = app.conf.getString("seata", "serverAddr", serverAddr);
        applicationId = app.conf.getString("server", "name", "unknown");
        txServiceGroup = app.conf.getString("seata", "txServiceGroup", txServiceGroup);
        
        if (enabled) {
            System.out.println("[Seata] 分布式事务已启用，TC地址: " + serverAddr);
        }
    }
    
    public String begin() {
        String xid = generateXid();
        GlobalTransaction gtx = new GlobalTransaction();
        gtx.xid = xid;
        gtx.status = TxStatus.BEGIN;
        gtx.startTime = System.currentTimeMillis();
        globalTransactions.put(xid, gtx);
        
        TxContext ctx = new TxContext();
        ctx.xid = xid;
        ctx.isGlobal = true;
        currentTx.set(ctx);
        
        return xid;
    }
    
    public void commit(String xid) {
        GlobalTransaction gtx = globalTransactions.get(xid);
        if (gtx == null) return;
        
        gtx.status = TxStatus.COMMITTING;
        
        for (BranchTransaction branch : gtx.branches) {
            try {
                commitBranch(branch);
            } catch (Exception e) {
                System.err.println("[Seata] 分支提交失败: " + branch.branchId);
            }
        }
        
        gtx.status = TxStatus.COMMITTED;
        globalTransactions.remove(xid);
        currentTx.remove();
    }
    
    public void rollback(String xid) {
        GlobalTransaction gtx = globalTransactions.get(xid);
        if (gtx == null) return;
        
        gtx.status = TxStatus.ROLLBACKING;
        
        List<BranchTransaction> branches = new ArrayList<>(gtx.branches);
        Collections.reverse(branches);
        
        for (BranchTransaction branch : branches) {
            try {
                rollbackBranch(branch);
            } catch (Exception e) {
                System.err.println("[Seata] 分支回滚失败: " + branch.branchId + ", " + e.getMessage());
            }
        }
        
        gtx.status = TxStatus.ROLLBACKED;
        globalTransactions.remove(xid);
        currentTx.remove();
    }
    
    public String registerBranch(String xid, Connection conn, String sql, Object[] params) {
        GlobalTransaction gtx = globalTransactions.get(xid);
        if (gtx == null) return null;
        
        String branchId = generateBranchId();
        BranchTransaction branch = new BranchTransaction();
        branch.branchId = branchId;
        branch.xid = xid;
        branch.resourceId = getResourceId(conn);
        branch.sql = sql;
        branch.params = params;
        branch.connection = conn;
        
        try {
            branch.beforeImage = captureBeforeImage(conn, sql, params);
        } catch (Exception e) {
            // ignore
        }
        
        gtx.branches.add(branch);
        return branchId;
    }
    
    public void branchComplete(String branchId, String xid) {
        GlobalTransaction gtx = globalTransactions.get(xid);
        if (gtx == null) return;
        
        for (BranchTransaction branch : gtx.branches) {
            if (branch.branchId.equals(branchId)) {
                try {
                    branch.afterImage = captureAfterImage(branch.connection, branch.sql, branch.params);
                    saveUndoLog(branch);
                } catch (Exception e) {
                    // ignore
                }
                break;
            }
        }
    }
    
    private void commitBranch(BranchTransaction branch) throws SQLException {
        if (branch.connection != null && !branch.connection.isClosed()) {
            try (PreparedStatement ps = branch.connection.prepareStatement(
                    "DELETE FROM undo_log WHERE branch_id = ? AND xid = ?")) {
                ps.setString(1, branch.branchId);
                ps.setString(2, branch.xid);
                ps.executeUpdate();
            }
        }
    }
    
    private void rollbackBranch(BranchTransaction branch) throws SQLException {
        if (branch.beforeImage == null || branch.connection == null) return;
        
        String undoSql = generateUndoSql(branch);
        if (undoSql != null && !branch.connection.isClosed()) {
            try (Statement stmt = branch.connection.createStatement()) {
                stmt.executeUpdate(undoSql);
            }
            commitBranch(branch);
        }
    }
    
    private Map<String, Object> captureBeforeImage(Connection conn, String sql, Object[] params) throws SQLException {
        String upperSql = sql.toUpperCase().trim();
        if (!upperSql.startsWith("UPDATE") && !upperSql.startsWith("DELETE")) {
            return null;
        }
        
        String tableName = extractTableName(sql);
        String whereClause = extractWhereClause(sql);
        if (tableName == null || whereClause == null) return null;
        
        String selectSql = "SELECT * FROM " + tableName + " WHERE " + whereClause;
        Map<String, Object> image = new HashMap<>();
        image.put("tableName", tableName);
        image.put("sql", sql);
        
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            if (params != null) {
                int whereParamStart = countParamsBeforeWhere(sql);
                for (int i = whereParamStart; i < params.length; i++) {
                    ps.setObject(i - whereParamStart + 1, params[i]);
                }
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
                image.put("rows", rows);
            }
        }
        return image;
    }
    
    private Map<String, Object> captureAfterImage(Connection conn, String sql, Object[] params) throws SQLException {
        String upperSql = sql.toUpperCase().trim();
        if (upperSql.startsWith("INSERT")) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (rs.next()) {
                    Map<String, Object> image = new HashMap<>();
                    image.put("tableName", extractTableName(sql));
                    image.put("insertedId", rs.getLong(1));
                    return image;
                }
            }
        }
        return captureBeforeImage(conn, sql, params);
    }
    
    private void saveUndoLog(BranchTransaction branch) throws SQLException {
        if (branch.connection == null || branch.connection.isClosed()) return;
        
        String sql = "INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created) VALUES (?, ?, ?, ?, 0, NOW())";
        try (PreparedStatement ps = branch.connection.prepareStatement(sql)) {
            ps.setString(1, branch.branchId);
            ps.setString(2, branch.xid);
            ps.setString(3, "serializer=json");
            ps.setString(4, serializeImage(branch.beforeImage, branch.afterImage));
            ps.executeUpdate();
        }
    }
    
    @SuppressWarnings("unchecked")
    private String generateUndoSql(BranchTransaction branch) {
        if (branch.beforeImage == null) return null;
        
        String tableName = (String) branch.beforeImage.get("tableName");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) branch.beforeImage.get("rows");
        if (tableName == null || rows == null || rows.isEmpty()) return null;
        
        Map<String, Object> row = rows.get(0);
        String originalSql = branch.sql.toUpperCase().trim();
        
        if (originalSql.startsWith("UPDATE")) {
            StringBuilder sb = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
            List<String> sets = new ArrayList<>();
            String pkColumn = "id";
            Object pkValue = row.get(pkColumn);
            
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (!entry.getKey().equalsIgnoreCase(pkColumn)) {
                    sets.add(entry.getKey() + " = " + formatValue(entry.getValue()));
                }
            }
            sb.append(String.join(", ", sets));
            sb.append(" WHERE ").append(pkColumn).append(" = ").append(formatValue(pkValue));
            return sb.toString();
        } else if (originalSql.startsWith("DELETE")) {
            StringBuilder sb = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
            List<String> columns = new ArrayList<>(row.keySet());
            sb.append(String.join(", ", columns));
            sb.append(") VALUES (");
            List<String> values = new ArrayList<>();
            for (String col : columns) {
                values.add(formatValue(row.get(col)));
            }
            sb.append(String.join(", ", values));
            sb.append(")");
            return sb.toString();
        }
        return null;
    }
    
    private String formatValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number) return value.toString();
        return "'" + value.toString().replace("'", "''") + "'";
    }
    
    private String extractTableName(String sql) {
        String upper = sql.toUpperCase();
        int start = -1;
        if (upper.contains("FROM ")) start = upper.indexOf("FROM ") + 5;
        else if (upper.contains("UPDATE ")) start = upper.indexOf("UPDATE ") + 7;
        else if (upper.contains("INTO ")) start = upper.indexOf("INTO ") + 5;
        if (start < 0) return null;
        
        int end = start;
        while (end < sql.length() && !Character.isWhitespace(sql.charAt(end))) end++;
        return sql.substring(start, end).trim();
    }
    
    private String extractWhereClause(String sql) {
        int idx = sql.toUpperCase().indexOf(" WHERE ");
        return idx < 0 ? null : sql.substring(idx + 7);
    }
    
    private int countParamsBeforeWhere(String sql) {
        int whereIdx = sql.toUpperCase().indexOf(" WHERE ");
        if (whereIdx < 0) return 0;
        int count = 0;
        for (int i = 0; i < whereIdx; i++) {
            if (sql.charAt(i) == '?') count++;
        }
        return count;
    }
    
    private String getResourceId(Connection conn) {
        try { return conn.getMetaData().getURL(); } catch (Exception e) { return "unknown"; }
    }
    
    private String serializeImage(Map<String, Object> before, Map<String, Object> after) {
        return "{\"before\":" + mapToJson(before) + ",\"after\":" + mapToJson(after) + "}";
    }
    
    @SuppressWarnings("unchecked")
    private String mapToJson(Map<String, Object> map) {
        if (map == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object v = entry.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number) sb.append(v);
            else if (v instanceof List) sb.append(listToJson((List<?>) v));
            else if (v instanceof Map) sb.append(mapToJson((Map<String, Object>) v));
            else sb.append("\"").append(v.toString().replace("\"", "\\\"")).append("\"");
            first = false;
        }
        return sb.append("}").toString();
    }
    
    @SuppressWarnings("unchecked")
    private String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            if (item instanceof Map) sb.append(mapToJson((Map<String, Object>) item));
            else sb.append("\"").append(item).append("\"");
            first = false;
        }
        return sb.append("]").toString();
    }
    
    private String generateXid() {
        return applicationId + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String generateBranchId() {
        return System.currentTimeMillis() + ":" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static String getCurrentXid() {
        TxContext ctx = currentTx.get();
        return ctx != null ? ctx.xid : null;
    }
    
    public static void bindXid(String xid) {
        TxContext ctx = new TxContext();
        ctx.xid = xid;
        ctx.isGlobal = false;
        currentTx.set(ctx);
    }
    
    public <T> T execute(Supplier<T> business) {
        String xid = begin();
        try {
            T result = business.get();
            commit(xid);
            return result;
        } catch (Exception e) {
            rollback(xid);
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
    
    static class TxContext { String xid; boolean isGlobal; }
    static class GlobalTransaction { String xid; TxStatus status; long startTime; List<BranchTransaction> branches = new CopyOnWriteArrayList<>(); }
    static class BranchTransaction { String branchId; String xid; String resourceId; String sql; Object[] params; Connection connection; Map<String, Object> beforeImage; Map<String, Object> afterImage; }
    enum TxStatus { BEGIN, COMMITTING, COMMITTED, ROLLBACKING, ROLLBACKED }
}
