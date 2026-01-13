package game.common;

import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 管理器 (BabyKylin 风格)
 * 
 * 简单的 token 生成和验证，用于 WebSocket 连接认证
 */
public class TokenManager {
    
    // token 有效期 (毫秒) - 5分钟
    private static final long TOKEN_LIFETIME = 5 * 60 * 1000;
    
    // token -> TokenInfo
    private static final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    
    // userId -> token (一个用户只能有一个有效 token)
    private static final Map<Long, String> userTokens = new ConcurrentHashMap<>();
    
    public static class TokenInfo {
        public long userId;
        public String roomId;
        public long createTime;
        public boolean valid = true;
    }
    
    /**
     * 生成 token (指定有效期)
     */
    public static String create(long userId, String roomId, long lifeTime) {
        return generate(userId, roomId);
    }
    
    /**
     * 生成 token
     */
    public static String generate(long userId, String roomId) {
        // 删除旧 token
        String oldToken = userTokens.get(userId);
        if (oldToken != null) {
            tokens.remove(oldToken);
        }
        
        // 生成新 token
        long time = System.currentTimeMillis();
        String token = md5(userId + "!@#$%^&" + time);
        
        TokenInfo info = new TokenInfo();
        info.userId = userId;
        info.roomId = roomId;
        info.createTime = time;
        
        tokens.put(token, info);
        userTokens.put(userId, token);
        
        return token;
    }
    
    /**
     * 解析并验证 token
     */
    public static TokenInfo parse(String token) {
        TokenInfo info = tokens.get(token);
        if (info == null) {
            TokenInfo invalid = new TokenInfo();
            invalid.valid = false;
            return invalid;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() - info.createTime > TOKEN_LIFETIME) {
            info.valid = false;
            tokens.remove(token);
            userTokens.remove(info.userId);
        }
        
        return info;
    }
    
    /**
     * 验证 token 是否有效
     */
    public static boolean isValid(String token) {
        TokenInfo info = tokens.get(token);
        if (info == null) {
            return false;
        }
        if (System.currentTimeMillis() - info.createTime > TOKEN_LIFETIME) {
            tokens.remove(token);
            userTokens.remove(info.userId);
            return false;
        }
        return true;
    }
    
    /**
     * 获取用户 ID
     */
    public static Long getUserId(String token) {
        TokenInfo info = tokens.get(token);
        return info != null ? info.userId : null;
    }
    
    /**
     * 获取房间 ID
     */
    public static String getRoomId(String token) {
        TokenInfo info = tokens.get(token);
        return info != null ? info.roomId : null;
    }
    
    /**
     * 标记 token 已使用 (删除)
     */
    public static void markUsed(String token) {
        TokenInfo info = tokens.remove(token);
        if (info != null) {
            userTokens.remove(info.userId);
        }
    }
    
    /**
     * 删除 token
     */
    public static void delete(String token) {
        markUsed(token);
    }
    
    /**
     * MD5 哈希
     */
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
