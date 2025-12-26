package example.dao;

import example.model.Session;
import example.infra.Db;

/**
 * 会话数据访问
 */
public class SessionDao {
    
    public Session findByToken(String token) {
        return Db.first(Session.class, "token = ?", token);
    }
    
    public String findUsernameByToken(String token) {
        Session s = findByToken(token);
        return s != null ? s.username : null;
    }
    
    public void create(String token, String username) {
        Session s = new Session();
        s.token = token;
        s.username = username;
        Db.create(s);
    }
    
    public void deleteByToken(String token) {
        Db.delete(Session.class, "token = ?", token);
    }
}
