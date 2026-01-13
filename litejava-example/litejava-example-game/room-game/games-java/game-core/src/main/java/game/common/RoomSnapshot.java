package game.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间快照 - 用于断线重连 (内存版)
 */
public class RoomSnapshot {
    
    private static final Map<Long, Snapshot> snapshots = new ConcurrentHashMap<>();
    
    public static class Snapshot {
        public long userId;
        public String serverId;
        public String roomId;
        public String gameType;
        public int seatIndex;
        public long createTime;
        public long expireTime;
        public Map<String, Object> extra = new HashMap<>();
    }
    
    public static void save(long userId, String serverId, String roomId, 
                           String gameType, int seatIndex, int expireSeconds) {
        Snapshot s = new Snapshot();
        s.userId = userId;
        s.serverId = serverId;
        s.roomId = roomId;
        s.gameType = gameType;
        s.seatIndex = seatIndex;
        s.createTime = System.currentTimeMillis();
        s.expireTime = s.createTime + expireSeconds * 1000L;
        snapshots.put(userId, s);
    }
    
    public static Snapshot get(long userId) {
        Snapshot s = snapshots.get(userId);
        if (s == null) return null;
        if (System.currentTimeMillis() > s.expireTime) {
            snapshots.remove(userId);
            return null;
        }
        return s;
    }
    
    public static void remove(long userId) {
        snapshots.remove(userId);
    }
    
    public static boolean hasUnfinishedGame(long userId) {
        return get(userId) != null;
    }
    
    public static void cleanExpired() {
        long now = System.currentTimeMillis();
        snapshots.entrySet().removeIf(e -> now > e.getValue().expireTime);
    }
}
