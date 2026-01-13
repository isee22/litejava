package game.common.redis;

import litejava.plugins.cache.RedisCachePlugin;

/**
 * 用户会话管理（Redis）
 * 
 * 存储 userId -> gatewayId 映射，支持跨 Gateway 踢人
 */
public class UserSession {
    
    private static final String KEY_PREFIX = "game:session:";
    private static final int EXPIRE_SECONDS = 24 * 60 * 60; // 24小时
    
    private static RedisCachePlugin redis;
    
    public static void init(RedisCachePlugin redis) {
        UserSession.redis = redis;
    }
    
    /**
     * 用户登录，记录 gatewayId
     * 
     * @return 旧的 gatewayId（如果有），用于踢人
     */
    public static String login(long userId, String gatewayId) {
        String key = KEY_PREFIX + userId;
        String oldGatewayId = redis.get(key);
        redis.set(key, gatewayId, EXPIRE_SECONDS);
        return oldGatewayId;
    }
    
    /**
     * 获取用户所在的 Gateway
     */
    public static String getGateway(long userId) {
        return redis.get(KEY_PREFIX + userId);
    }
    
    /**
     * 用户登出
     */
    public static void logout(long userId) {
        redis.del(KEY_PREFIX + userId);
    }
    
    /**
     * 刷新会话有效期
     */
    public static void refresh(long userId) {
        redis.expire(KEY_PREFIX + userId, EXPIRE_SECONDS);
    }
    
    /**
     * 检查用户是否在线
     */
    public static boolean isOnline(long userId) {
        return redis.exists(KEY_PREFIX + userId);
    }
}
