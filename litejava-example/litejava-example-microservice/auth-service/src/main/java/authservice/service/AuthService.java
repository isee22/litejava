package authservice.service;

import authservice.G;
import authservice.model.Account;
import common.BizException;
import common.Err;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.DiscoveryPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 认证服务
 */
public class AuthService {
    
    public static Map<String, Object> login(String username, String password) {
        Account account = G.accountMapper.findByUsername(username);
        if (account == null) BizException.error(Err.USER_NOT_FOUND, "用户不存在");
        if (!account.passwordHash.equals(password)) BizException.error(Err.PASSWORD_ERROR, "密码错误");
        
        G.accountMapper.updateLastLogin(account.userId);
        
        String token = G.jwt.generateToken(account.userId, account.username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", account.userId);
        result.put("username", account.username);
        return result;
    }
    
    public static Map<String, Object> register(Long userId, String username, String password) {
        if (G.accountMapper.findByUsername(username) != null) BizException.error(Err.USER_EXISTS, "用户名已存在");
        
        Account account = new Account();
        account.userId = userId;
        account.username = username;
        account.passwordHash = password;
        
        G.accountMapper.insert(account);
        
        String token = G.jwt.generateToken(userId, username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", userId);
        result.put("username", username);
        return result;
    }
    
    /**
     * 注册并创建用户（调用 user-service）
     */
    public static Map<String, Object> registerWithUserCreation(String username, String password, String email, String phone) {
        if (G.accountMapper.findByUsername(username) != null) BizException.error(Err.USER_EXISTS, "用户名已存在");
        
        ConsulPlugin consul = G.consul();
        DiscoveryPlugin.ServiceInstance instance = consul.getInstance("user-service");
        if (instance == null) BizException.error(Err.SERVER_ERROR, "user-service 不可用");
        
        String userServiceUrl = instance.getUrl();
        String requestBody = String.format(
            "{\"username\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\"}",
            username, email != null ? email : "", phone != null ? phone : ""
        );
        
        String responseBody = httpPost(userServiceUrl + "/user/create", requestBody);
        
        Long userId = parseUserId(responseBody);
        if (userId == null) BizException.error(Err.SERVER_ERROR, "创建用户失败: 无法获取用户ID");
        
        Account account = new Account();
        account.userId = userId;
        account.username = username;
        account.passwordHash = password;
        G.accountMapper.insert(account);
        
        String token = G.jwt.generateToken(userId, username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", userId);
        result.put("username", username);
        return result;
    }
    
    /** 简单 HTTP POST */
    private static String httpPost(String urlStr, String body) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            
            int code = conn.getResponseCode();
            try (Scanner scanner = new Scanner(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8.name())) {
                String resp = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                if (code != 200) BizException.error(Err.SERVER_ERROR, "HTTP 调用失败: " + resp);
                return resp;
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            BizException.error(Err.SERVER_ERROR, "HTTP 调用异常: " + e.getMessage());
            return null;
        }
    }
    
    private static Long parseUserId(String json) {
        // 简单解析 "id":123 或 "id": 123
        int idx = json.indexOf("\"id\"");
        if (idx == -1) return null;
        
        int colonIdx = json.indexOf(":", idx);
        if (colonIdx == -1) return null;
        
        int start = colonIdx + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }
        
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        
        if (end > start) {
            return Long.parseLong(json.substring(start, end));
        }
        return null;
    }
    
    public static Map<String, Object> validate(String token) {
        return G.jwt.validateToken(token);
    }
    
    public static boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Account account = G.accountMapper.findByUserId(userId);
        if (account == null) BizException.error(Err.USER_NOT_FOUND, "账号不存在");
        if (!account.passwordHash.equals(oldPassword)) BizException.error(Err.PASSWORD_ERROR, "旧密码错误");
        
        return G.accountMapper.updatePassword(userId, newPassword) > 0;
    }
    
    public static String refreshToken(String token) {
        Map<String, Object> result = G.jwt.validateToken(token);
        if (!Boolean.TRUE.equals(result.get("valid"))) BizException.error(Err.TOKEN_EXPIRED, "Token 无效或已过期");
        
        Long userId = (Long) result.get("userId");
        String username = (String) result.get("username");
        
        return G.jwt.generateToken(userId, username);
    }
}
