package example.dao;

import example.model.User;
import litejava.plugins.database.JdbcPlugin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

/**
 * 用户数据访问 - 使用原生 JdbcTemplate
 */
public class UserDao {
    
    private JdbcTemplate jdbc() {
        return JdbcPlugin.instance.jdbcTemplate;
    }
    
    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.id = rs.getLong("id");
        user.username = rs.getString("username");
        user.password = rs.getString("password");
        user.createdAt = rs.getTimestamp("created_at");
        return user;
    };
    
    public User findById(long id) {
        List<User> list = jdbc().query(
            "SELECT * FROM users WHERE id = ?", ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }
    
    public User findByCredentials(String username, String password) {
        List<User> list = jdbc().query(
            "SELECT * FROM users WHERE username = ? AND password = ?",
            ROW_MAPPER, username, password);
        return list.isEmpty() ? null : list.get(0);
    }
    
    public User findByUsername(String username) {
        List<User> list = jdbc().query(
            "SELECT * FROM users WHERE username = ?", ROW_MAPPER, username);
        return list.isEmpty() ? null : list.get(0);
    }
    
    public void create(User user) {
        jdbc().update(
            "INSERT INTO users (username, password) VALUES (?, ?)",
            user.username, user.password);
    }
    
    public void create(String username, String password) {
        jdbc().update(
            "INSERT INTO users (username, password) VALUES (?, ?)",
            username, password);
    }
}
