package example.service;

import example.Dao;

import java.util.UUID;

/**
 * 认证服务
 */
public class AuthService {
    
    public String login(String username, String password) {
        if (Dao.user.findByCredentials(username, password) != null) {
            String token = UUID.randomUUID().toString();
            Dao.session.create(token, username);
            return token;
        }
        return null;
    }
    
    public boolean register(String username, String password) {
        if (Dao.user.findByUsername(username) != null) {
            return false;
        }
        Dao.user.create(username, password);
        return true;
    }
    
    public String validateToken(String token) {
        return Dao.session.findUsernameByToken(token);
    }
    
    public void logout(String token) {
        Dao.session.deleteByToken(token);
    }
}
