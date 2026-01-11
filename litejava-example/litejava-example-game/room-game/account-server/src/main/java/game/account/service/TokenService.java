package game.account.service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Token 服务
 * 
 * 负责生成和验证用户 token
 */
public class TokenService {
    
    // token -> userId
    private static final Map<String, Long> tokenToUser = new ConcurrentHashMap<>();
    // userId -> token
    private static final Map<Long, String> userToToken = new ConcurrentHashMap<>();
    // token -> 过期时间
    private static final Map<String, Long> tokenExpire = new ConcurrentHashMap<>();
    
    // token 有效期 (24小时)
    private static final long TOKEN_TTL = 24 * 60 * 60 * 1000L;
    
    /**
     * 生成 token
     */
    public static String generate(long userId) {
        // 移除旧 token
        String oldToken = userToToken.get(userId);
        if (oldToken != null) {
            tokenToUser.remove(oldToken);
            tokenExpire.remove(oldToken);
        }
        
        // 生成新 token
        String token = UUID.randomUUID().toString().replace("-", "");
        long expireTime = System.currentTimeMillis() + TOKEN_TTL;
        
        tokenToUser.put(token, userId);
        userToToken.put(userId, token);
        tokenExpire.put(token, expireTime);
        
        return token;
    }
    
    /**
     * 验证 token，返回 userId，无效返回 null
     */
    public static Long verify(String token) {
        if (token == null) return null;
        
        Long expireTime = tokenExpire.get(token);
        if (expireTime == null || System.currentTimeMillis() > expireTime) {
            // 过期，清理
            Long userId = tokenToUser.remove(token);
            tokenExpire.remove(token);
            if (userId != null) {
                userToToken.remove(userId);
            }
            return null;
        }
        
        return tokenToUser.get(token);
    }
    
    /**
     * 获取用户的 token
     */
    public static String getToken(long userId) {
        return userToToken.get(userId);
    }
    
    /**
     * 移除 token
     */
    public static void remove(long userId) {
        String token = userToToken.remove(userId);
        if (token != null) {
            tokenToUser.remove(token);
            tokenExpire.remove(token);
        }
    }
    
    /**
     * 刷新 token 有效期
     */
    public static void refresh(String token) {
        if (tokenToUser.containsKey(token)) {
            tokenExpire.put(token, System.currentTimeMillis() + TOKEN_TTL);
        }
    }
}
