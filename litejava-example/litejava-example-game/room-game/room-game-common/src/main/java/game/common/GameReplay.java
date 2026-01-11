package game.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏录像 - 用于回放 (内存版)
 */
public class GameReplay {
    
    private static final Map<String, Replay> replays = new ConcurrentHashMap<>();
    private static final Map<Long, List<String>> userReplays = new ConcurrentHashMap<>();
    private static final int MAX_USER_REPLAYS = 20;
    
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
    
    public static void setInitState(String replayId, Map<String, Object> initState) {
        Replay r = replays.get(replayId);
        if (r != null) {
            r.initState = initState;
        }
    }
    
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
    
    public static void finish(String replayId, int winner) {
        Replay r = replays.get(replayId);
        if (r == null) return;
        
        r.endTime = System.currentTimeMillis();
        r.winner = winner;
        
        for (long userId : r.players) {
            List<String> list = userReplays.computeIfAbsent(userId, k -> new ArrayList<>());
            list.add(0, replayId);
            if (list.size() > MAX_USER_REPLAYS) {
                String oldId = list.remove(list.size() - 1);
                replays.remove(oldId);
            }
        }
    }
    
    public static Replay get(String replayId) {
        return replays.get(replayId);
    }
    
    public static List<Replay> getUserReplays(long userId) {
        List<String> ids = userReplays.get(userId);
        if (ids == null) return Collections.emptyList();
        
        List<Replay> result = new ArrayList<>();
        for (String id : ids) {
            Replay r = replays.get(id);
            if (r != null) result.add(r);
        }
        return result;
    }
    
    public static List<ReplaySummary> getUserReplaySummaries(long userId) {
        List<Replay> list = getUserReplays(userId);
        List<ReplaySummary> result = new ArrayList<>();
        for (Replay r : list) {
            ReplaySummary summary = ReplaySummary.from(r);
            result.add(summary);
        }
        return result;
    }
    
    public static boolean delete(String replayId) {
        Replay r = replays.remove(replayId);
        if (r == null) return false;
        
        for (long userId : r.players) {
            List<String> list = userReplays.get(userId);
            if (list != null) {
                list.remove(replayId);
            }
        }
        return true;
    }
    
    public static int getTotalCount() {
        return replays.size();
    }
}
