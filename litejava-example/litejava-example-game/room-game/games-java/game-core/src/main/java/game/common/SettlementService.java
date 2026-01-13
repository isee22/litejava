package game.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * 结算服务 - 异步调用 AccountServer，失败自动重试
 */
public class SettlementService {
    
    /**
     * 日志回调接口
     */
    public interface LogCallback {
        void info(String msg);
        void error(String msg);
    }
    
    /**
     * 玩家结算数据
     */
    public static class PlayerSettlement {
        public long userId;
        public boolean win;
        public int score;
        public int coinChange;
        public int exp;
        
        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", userId);
            m.put("win", win);
            m.put("score", score);
            m.put("coinChange", coinChange);
            m.put("exp", exp);
            return m;
        }
    }
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private final String accountUrl;
    private final LogCallback log;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ConcurrentLinkedQueue<Map<String, Object>> retryQueue = new ConcurrentLinkedQueue<>();
    
    public SettlementService(String accountUrl) {
        this(accountUrl, null);
    }
    
    public SettlementService(String accountUrl, LogCallback log) {
        this.accountUrl = accountUrl;
        this.log = log;
        // 每5秒处理所有失败的任务
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            Map<String, Object> task;
            while ((task = retryQueue.poll()) != null) {
                Map<String, Object> t = task;
                executor.submit(() -> doSettle(t));
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 提交结算 (立即异步执行)
     */
    public void submit(String roomId, String gameType, List<Map<String, Object>> settlements) {
        Map<String, Object> task = new HashMap<>();
        task.put("roomId", roomId);
        task.put("gameType", gameType);
        task.put("settlements", settlements);
        executor.submit(() -> doSettle(task));
    }
    
    /**
     * 提交结算 (使用 PlayerSettlement 列表)
     */
    public void submit(String roomId, String gameType, List<PlayerSettlement> settlements, boolean useVO) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PlayerSettlement s : settlements) {
            list.add(s.toMap());
        }
        submit(roomId, gameType, list);
    }
    
    private void doSettle(Map<String, Object> task) {
        if (httpPost(accountUrl + "/game/settle", task)) {
            logInfo("成功: " + task.get("roomId"));
        } else {
            retryQueue.offer(task);
        }
    }
    
    private void logInfo(String msg) {
        if (log != null) {
            log.info(msg);
        } else {
            System.out.println("[Settlement] " + msg);
        }
    }
    
    private void logError(String msg) {
        if (log != null) {
            log.error(msg);
        } else {
            System.err.println("[Settlement] " + msg);
        }
    }
    
    @SuppressWarnings("unchecked")
    private boolean httpPost(String urlStr, Map<String, Object> body) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.getOutputStream().write(mapper.writeValueAsBytes(body));
            
            if (conn.getResponseCode() == 200) {
                Map<String, Object> resp = mapper.readValue(conn.getInputStream(), Map.class);
                return resp.get("code") != null && ((Number) resp.get("code")).intValue() == 0;
            }
        } catch (Exception e) {
            logError("HTTP 请求失败: " + e.getMessage());
        }
        return false;
    }
}
