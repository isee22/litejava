package game.common.redis;

import litejava.plugins.cache.RedisCachePlugin;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * Redis 匹配队列
 * 
 * 支持 Match Server 集群，所有实例共享同一个队列
 */
public class RedisMatchQueue {
    
    private static final String QUEUE_PREFIX = "game:match:queue:";
    private static final String USER_PREFIX = "game:match:user:";
    private static final String RESULT_PREFIX = "game:match:result:";
    private static final int USER_EXPIRE = 300; // 5分钟
    private static final int RESULT_EXPIRE = 300; // 5分钟
    
    private static RedisCachePlugin redis;
    
    public static void init(RedisCachePlugin redis) {
        RedisMatchQueue.redis = redis;
    }
    
    /**
     * 加入匹配队列
     */
    public static boolean join(String queueKey, long userId, String userData) {
        String userKey = USER_PREFIX + userId;
        
        if (redis.exists(userKey)) {
            return false;
        }
        
        redis.set(userKey, queueKey + "|" + userData, USER_EXPIRE);
        
        try (Jedis jedis = redis.getJedis()) {
            jedis.rpush(QUEUE_PREFIX + queueKey, String.valueOf(userId));
        }
        
        return true;
    }
    
    /**
     * 取消匹配
     */
    public static boolean cancel(long userId) {
        String userKey = USER_PREFIX + userId;
        String data = redis.get(userKey);
        
        if (data == null) {
            return false;
        }
        
        int idx = data.indexOf("|");
        if (idx > 0) {
            String queueKey = data.substring(0, idx);
            try (Jedis jedis = redis.getJedis()) {
                jedis.lrem(QUEUE_PREFIX + queueKey, 1, String.valueOf(userId));
            }
        }
        
        redis.del(userKey);
        return true;
    }
    
    /**
     * 检查用户是否在匹配中
     */
    public static boolean isMatching(long userId) {
        return redis.exists(USER_PREFIX + userId);
    }
    
    /**
     * 获取用户匹配数据
     */
    public static String getUserData(long userId) {
        String data = redis.get(USER_PREFIX + userId);
        if (data == null) return null;
        
        int idx = data.indexOf("|");
        return idx > 0 ? data.substring(idx + 1) : null;
    }
    
    /**
     * 尝试匹配
     */
    public static List<Long> tryMatch(String queueKey, int needPlayers) {
        String key = QUEUE_PREFIX + queueKey;
        
        try (Jedis jedis = redis.getJedis()) {
            long size = jedis.llen(key);
            if (size < needPlayers) {
                return null;
            }
            
            List<Long> matched = new ArrayList<>();
            for (int i = 0; i < needPlayers; i++) {
                String userIdStr = jedis.lpop(key);
                if (userIdStr == null) break;
                
                long userId = Long.parseLong(userIdStr);
                
                if (redis.exists(USER_PREFIX + userId)) {
                    matched.add(userId);
                } else {
                    i--;
                }
            }
            
            if (matched.size() < needPlayers) {
                for (Long userId : matched) {
                    jedis.lpush(key, String.valueOf(userId));
                }
                return null;
            }
            
            for (Long userId : matched) {
                redis.del(USER_PREFIX + userId);
            }
            
            return matched;
        }
    }
    
    /**
     * 获取队列长度
     */
    public static long getQueueSize(String queueKey) {
        try (Jedis jedis = redis.getJedis()) {
            return jedis.llen(QUEUE_PREFIX + queueKey);
        }
    }
    
    /**
     * 清理用户
     */
    public static void removeUser(long userId) {
        cancel(userId);
    }
    
    /**
     * 检查用户是否在队列中
     */
    public static boolean isInQueue(long userId) {
        return redis.exists(USER_PREFIX + userId);
    }
    
    /**
     * 保存匹配结果 (客户端轮询获取)
     */
    public static void saveMatchResult(long userId, String resultJson) {
        try (Jedis jedis = redis.getJedis()) {
            jedis.setex(RESULT_PREFIX + userId, RESULT_EXPIRE, resultJson);
        }
    }
    
    /**
     * 获取匹配结果 (获取后删除)
     */
    public static String getMatchResult(long userId) {
        String key = RESULT_PREFIX + userId;
        try (Jedis jedis = redis.getJedis()) {
            String json = jedis.get(key);
            if (json == null) return null;
            jedis.del(key);
            return json;
        }
    }
}
