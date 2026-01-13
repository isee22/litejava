package game.common;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * 游戏录像 - 支持内存和数据库存储
 * 
 * 内存用于游戏中记录动作，游戏结束后异步保存到数据库
 */
public class GameReplay {
    
    // 内存缓存 (游戏中使用)
    private static final Map<String, Replay> replays = new ConcurrentHashMap<>();
    
    // AccountServer URL (用于持久化)
    private static String accountServerUrl = "http://localhost:8101";
    
    // 异步保存线程池
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // JSON 工具 (简单实现)
    private static JsonHelper json;
    
    public static void init(String accountUrl, JsonHelper jsonHelper) {
        accountServerUrl = accountUrl;
        json = jsonHelper;
    }
    
    public interface JsonHelper {
        String stringify(Object obj);
        Map<String, Object> parseMap(String str);
    }
    
    public static class Replay {
        public String replayId;
        public String gameType;
        public String roomId;
        public long startTime;
        public long endTime;
        public List<Long> players = new ArrayList<>();
        public List<String> playerNames = new ArrayList<>();
        public int winner;
        public List<Action> actions = new ArrayList<>();
        public Map<String, Object> initState;
    }
    
    public static class Action {
        public int frame;
        public long userId;
        public int cmd;
        public Map<String, Object> data;
        public long timestamp;
    }
    
    public static class ReplaySummary {
        public String replayId;
        public String gameType;
        public long startTime;
        public long endTime;
        public List<String> playerNames;
        public int winner;
        public int actionCount;
        
        public static ReplaySummary from(Replay r) {
            ReplaySummary s = new ReplaySummary();
            s.replayId = r.replayId;
            s.gameType = r.gameType;
            s.startTime = r.startTime;
            s.endTime = r.endTime;
            s.playerNames = r.playerNames;
            s.winner = r.winner;
            s.actionCount = r.actions.size();
            return s;
        }
    }
    
    /**
     * 创建录像 (内存)
     */
    public static Replay create(String gameType, String roomId, List<Long> players, List<String> names) {
        Replay r = new Replay();
        r.replayId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        r.gameType = gameType;
        r.roomId = roomId;
        r.startTime = System.currentTimeMillis();
        r.players.addAll(players);
        r.playerNames.addAll(names);
        replays.put(r.replayId, r);
        return r;
    }
    
    /**
     * 设置初始状态
     */
    public static void setInitState(String replayId, Map<String, Object> initState) {
        Replay r = replays.get(replayId);
        if (r != null) {
            r.initState = initState;
        }
    }
    
    /**
     * 记录动作 (内存)
     */
    public static void record(String replayId, int frame, long userId, int cmd, Object data) {
        Replay r = replays.get(replayId);
        if (r == null) return;
        
        Action a = new Action();
        a.frame = frame;
        a.userId = userId;
        a.cmd = cmd;
        if (data instanceof Map) {
            a.data = new HashMap<>((Map<String, Object>) data);
        }
        a.timestamp = System.currentTimeMillis();
        r.actions.add(a);
    }
    
    /**
     * 完成录像并异步保存到数据库
     */
    public static void finish(String replayId, int winner) {
        Replay r = replays.remove(replayId);
        if (r == null) return;
        
        r.endTime = System.currentTimeMillis();
        r.winner = winner;
        
        // 异步保存到数据库
        executor.submit(() -> saveToDb(r));
    }
    
    /**
     * 保存到数据库 (通过 AccountServer)
     */
    private static void saveToDb(Replay r) {
        if (json == null) return;
        
        try {
            // 1. 创建录像记录
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("replayId", r.replayId);
            createReq.put("gameType", r.gameType);
            createReq.put("roomId", r.roomId);
            createReq.put("players", r.players);
            createReq.put("names", r.playerNames);
            createReq.put("initState", r.initState != null ? json.stringify(r.initState) : "{}");
            
            httpPost(accountServerUrl + "/replay/create", json.stringify(createReq));
            
            // 2. 完成录像
            Map<String, Object> finishReq = new HashMap<>();
            finishReq.put("replayId", r.replayId);
            finishReq.put("winner", r.winner);
            finishReq.put("actions", json.stringify(r.actions));
            finishReq.put("actionCount", r.actions.size());
            
            httpPost(accountServerUrl + "/replay/finish", json.stringify(finishReq));
            
        } catch (Exception e) {
            System.err.println("[GameReplay] 保存录像失败: " + e.getMessage());
        }
    }
    
    private static void httpPost(String url, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP " + code);
        }
        conn.disconnect();
    }
    
    /**
     * 获取录像 (内存中的)
     */
    public static Replay get(String replayId) {
        return replays.get(replayId);
    }
    
    /**
     * 获取内存中的录像数量
     */
    public static int getTotalCount() {
        return replays.size();
    }
}
