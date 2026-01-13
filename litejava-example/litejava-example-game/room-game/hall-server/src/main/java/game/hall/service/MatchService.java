package game.hall.service;

import game.common.ErrCode;
import game.common.GameException;
import game.hall.G;
import game.hall.cache.CacheKeys;
import game.hall.vo.MatchUserVO;
import game.hall.vo.RoomResultVO;
import litejava.App;
import litejava.plugins.cache.RedisCachePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 匹配服务
 */
public class MatchService {
    
    public App app = G.app;
    public RedisCachePlugin cache = G.cache;
    public RoomService roomService = G.roomService;
    
    /** 匹配所需人数 */
    public int matchSize = 2;
    
    /** 段位匹配范围 (初始) */
    public int rankRange = 100;
    
    /** 段位匹配范围扩展 (每30秒) */
    public int rankRangeExpand = 50;
    
    public void joinQueue(long userId, String name, String gameType, String conf, int rank) {
        String existingRoom = cache.get(CacheKeys.user(userId));
        if (existingRoom != null) {
            GameException.error(ErrCode.ROOM_ALREADY_IN, "already in room");
        }
        
        String existingMatch = cache.get(CacheKeys.matchUser(userId));
        if (existingMatch != null) {
            GameException.error(ErrCode.INVALID_ACTION, "already in match queue");
        }
        
        MatchUserVO info = new MatchUserVO();
        info.userId = userId;
        info.name = name;
        info.gameType = gameType;
        info.conf = conf;
        info.rank = rank;
        info.joinTime = System.currentTimeMillis();
        
        cache.set(CacheKeys.matchUser(userId), app.json.stringify(info), CacheKeys.MATCH_EXPIRE);
        cache.zadd(CacheKeys.matchQueue(gameType), userId, info.joinTime);
        
        app.log.info("加入匹配: userId=" + userId + ", gameType=" + gameType + ", rank=" + rank);
    }
    
    public void cancelMatch(long userId, String gameType) {
        cache.del(CacheKeys.matchUser(userId));
        cache.zrem(CacheKeys.matchQueue(gameType), String.valueOf(userId));
        app.log.info("取消匹配: userId=" + userId);
    }
    
    public MatchUserVO getMatchStatus(long userId) {
        String json = cache.get(CacheKeys.matchUser(userId));
        if (json == null) return null;
        return app.json.parse(json, MatchUserVO.class);
    }
    
    public void doMatch(String gameType) {
        cache.withLock(CacheKeys.lockMatch(gameType), CacheKeys.LOCK_EXPIRE, () -> {
            doMatchInternal(gameType);
            return null;
        });
    }
    
    private void doMatchInternal(String gameType) {
        Set<String> userIds = cache.zrange(CacheKeys.matchQueue(gameType), 0, -1);
        if (userIds == null || userIds.size() < matchSize) return;
        
        List<MatchUserVO> queue = new ArrayList<>();
        for (String odUserId : userIds) {
            MatchUserVO info = getMatchStatus(Long.parseLong(odUserId));
            if (info != null) queue.add(info);
        }
        
        List<MatchUserVO> matched = new ArrayList<>();
        for (MatchUserVO user : queue) {
            if (matched.size() >= matchSize) break;
            
            long waitTime = System.currentTimeMillis() - user.joinTime;
            int expandTimes = (int) (waitTime / 30000);
            int currentRange = rankRange + expandTimes * rankRangeExpand;
            
            if (matched.isEmpty()) {
                matched.add(user);
            } else {
                MatchUserVO first = matched.get(0);
                if (Math.abs(user.rank - first.rank) <= currentRange) {
                    matched.add(user);
                }
            }
        }
        
        if (matched.size() >= matchSize) {
            createMatchRoom(gameType, matched);
        }
    }
    
    private void createMatchRoom(String gameType, List<MatchUserVO> players) {
        if (players.isEmpty()) return;
        
        MatchUserVO first = players.get(0);
        
        try {
            RoomResultVO room = roomService.createRoom(first.userId, gameType);
            
            for (int i = 1; i < players.size(); i++) {
                MatchUserVO player = players.get(i);
                try {
                    roomService.joinRoom(player.userId, room.roomId);
                } catch (Exception e) {
                    app.log.warn("匹配玩家加入失败: userId=" + player.userId + ", " + e.getMessage());
                }
            }
            
            for (MatchUserVO player : players) {
                cache.del(CacheKeys.matchUser(player.userId));
                cache.zrem(CacheKeys.matchQueue(gameType), String.valueOf(player.userId));
            }
            
            app.log.info("匹配成功: gameType=" + gameType + ", players=" + players.size() + ", roomId=" + room.roomId);
            
        } catch (Exception e) {
            app.log.error("创建匹配房间失败: " + e.getMessage());
        }
    }
    
    public void cleanupExpired(String gameType) {
        long expireTime = System.currentTimeMillis() - CacheKeys.MATCH_EXPIRE * 1000L;
        Set<String> expired = cache.zrangeByScore(CacheKeys.matchQueue(gameType), 0, expireTime);
        
        if (expired != null) {
            for (String odUserId : expired) {
                cache.del(CacheKeys.matchUser(Long.parseLong(odUserId)));
                cache.zrem(CacheKeys.matchQueue(gameType), odUserId);
                app.log.info("匹配超时移除: userId=" + odUserId);
            }
        }
    }
}
