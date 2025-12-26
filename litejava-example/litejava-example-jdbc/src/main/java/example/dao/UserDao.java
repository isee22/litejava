package example.dao;

import example.model.User;
import example.infra.Db;

/**
 * 用户数据访问
 */
public class UserDao {
    
    public User findById(long id) {
        return Db.first(User.class, id);
    }
    
    public User findByCredentials(String username, String password) {
        return Db.first(User.class, "username = ? AND password = ?", username, password);
    }
    
    public User findByUsername(String username) {
        return Db.first(User.class, "username = ?", username);
    }
    
    public void create(User user) {
        Db.create(user);
    }
    
    public void create(String username, String password) {
        User user = new User();
        user.username = username;
        user.password = password;
        Db.create(user);
    }
}
