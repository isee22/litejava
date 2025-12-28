package example.infra;

import litejava.plugins.database.JdbcPlugin;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作工具类 - 简洁的 CRUD API
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 查询
 * List<Book> books = Db.find(Book.class);
 * Book book = Db.first(Book.class, 1);
 * 
 * // 增删改
 * Db.create(book);
 * Db.update(book);
 * Db.delete(Book.class, 1);
 * 
 * // 原生 SQL
 * List<Book> books = Db.query(Book.class, "SELECT * FROM books WHERE title LIKE ?", "%Java%");
 * Db.exec("UPDATE books SET title = ? WHERE id = ?", "New Title", 1);
 * 
 * // 事务
 * Db.tx(status -> {
 *     Db.create(book);
 *     Db.update(inventory);
 *     return null;
 * });
 * }</pre>
 * 
 * <h2>Model 定义</h2>
 * <pre>{@code
 * @Table(name = "books")
 * public class Book {
 *     public Long id;
 *     public String title;
 *     
 *     @Column(name = "cover_image")
 *     public String coverImage;
 * }
 * }</pre>
 */
public class Db {
    
    // ==================== 查询 ====================
    
    public static <T> List<T> find(Class<T> clazz) {
        return jdbc().query("SELECT * FROM " + tableName(clazz), rowMapper(clazz));
    }
    
    public static <T> List<T> find(Class<T> clazz, String where, Object... args) {
        return jdbc().query("SELECT * FROM " + tableName(clazz) + " WHERE " + where, 
            rowMapper(clazz), args);
    }
    
    public static <T> T first(Class<T> clazz, Object id) {
        List<T> list = jdbc().query("SELECT * FROM " + tableName(clazz) + " WHERE id = ? LIMIT 1",
            rowMapper(clazz), id);
        return list.isEmpty() ? null : list.get(0);
    }
    
    public static <T> T first(Class<T> clazz, String where, Object... args) {
        List<T> list = jdbc().query("SELECT * FROM " + tableName(clazz) + " WHERE " + where + " LIMIT 1",
            rowMapper(clazz), args);
        return list.isEmpty() ? null : list.get(0);
    }
    
    // ==================== 增删改 ====================
    
    public static int create(Object entity) {
        Class<?> clazz = entity.getClass();
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        
        for (Field f : clazz.getFields()) {
            if ("id".equals(f.getName())) continue;
            try {
                Object val = f.get(entity);
                if (val != null) {
                    columns.add(columnName(f));
                    values.add(val);
                }
            } catch (IllegalAccessException ignored) {}
        }
        
        String sql = "INSERT INTO " + tableName(clazz) + " (" +
            String.join(", ", columns) + ") VALUES (" +
            String.join(", ", repeat("?", columns.size())) + ")";
        
        return jdbc().update(sql, values.toArray());
    }
    
    public static int update(Object entity) {
        Class<?> clazz = entity.getClass();
        List<String> sets = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        Object id = null;
        
        for (Field f : clazz.getFields()) {
            try {
                Object val = f.get(entity);
                if ("id".equals(f.getName())) {
                    id = val;
                } else if (val != null) {
                    sets.add(columnName(f) + " = ?");
                    values.add(val);
                }
            } catch (IllegalAccessException ignored) {}
        }
        
        if (id == null) throw new IllegalArgumentException("Entity must have id field");
        values.add(id);
        
        return jdbc().update("UPDATE " + tableName(clazz) + " SET " + 
            String.join(", ", sets) + " WHERE id = ?", values.toArray());
    }
    
    public static int delete(Class<?> clazz, Object id) {
        return jdbc().update("DELETE FROM " + tableName(clazz) + " WHERE id = ?", id);
    }
    
    public static int delete(Class<?> clazz, String where, Object... args) {
        return jdbc().update("DELETE FROM " + tableName(clazz) + " WHERE " + where, args);
    }
    
    // ==================== 原生 SQL ====================
    
    public static <T> List<T> query(Class<T> clazz, String sql, Object... args) {
        return jdbc().query(sql, rowMapper(clazz), args);
    }
    
    public static int exec(String sql, Object... args) {
        return jdbc().update(sql, args);
    }
    
    public static long count(Class<?> clazz) {
        Long result = jdbc().queryForObject("SELECT COUNT(*) FROM " + tableName(clazz), Long.class);
        return result != null ? result : 0;
    }
    
    // ==================== 事务 ====================
    
    public static <T> T tx(TransactionCallback<T> action) {
        return JdbcPlugin.instance.txTemplate.execute(action);
    }
    
    // ==================== 底层访问 ====================
    
    public static JdbcTemplate jdbc() {
        return JdbcPlugin.instance.jdbcTemplate;
    }
    
    public static TransactionTemplate tx() {
        return JdbcPlugin.instance.txTemplate;
    }
    
    // ==================== 内部方法 ====================
    
    private static String tableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        // 默认: Book -> books
        return toSnakeCase(clazz.getSimpleName()) + "s";
    }
    
    private static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        // 默认: coverImage -> cover_image
        return toSnakeCase(field.getName());
    }
    
    private static String toSnakeCase(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static List<String> repeat(String s, int n) {
        List<String> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(s);
        return list;
    }
    
    /**
     * 创建支持 public 字段的 RowMapper
     */
    private static <T> RowMapper<T> rowMapper(Class<T> clazz) {
        // 构建字段映射: column_name -> Field
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field f : clazz.getFields()) {
            String colName = columnName(f).toLowerCase();
            fieldMap.put(colName, f);
            // 也支持不带下划线的列名
            fieldMap.put(colName.replace("_", ""), f);
        }
        
        return (rs, rowNum) -> {
            try {
                T obj = clazz.getDeclaredConstructor().newInstance();
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                
                for (int i = 1; i <= colCount; i++) {
                    String colName = meta.getColumnLabel(i).toLowerCase();
                    Field field = fieldMap.get(colName);
                    if (field == null) {
                        field = fieldMap.get(colName.replace("_", ""));
                    }
                    if (field != null) {
                        Object value = rs.getObject(i);
                        if (value != null) {
                            field.set(obj, value);
                        }
                    }
                }
                return obj;
            } catch (Exception e) {
                throw new SQLException("Failed to map row to " + clazz.getName(), e);
            }
        };
    }
}
