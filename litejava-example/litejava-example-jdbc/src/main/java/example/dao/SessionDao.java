package example.dao;

import example.model.Session;
import litejava.plugins.database.JdbcPlugin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

/**
 * 会话数据访问 - 使用原生 JdbcTemplate
 */
public class SessionDao {
    
    private JdbcTemplate jdbc() {
        return JdbcPlugin.instance.jdbcTemplate;
    }
    
    private static final RowMapper<Session> ROW_MAPPER = (rs, rowNum) -> {
        Session s = new Session();
        s.id = rs.getLong("id");
        s.token = rs.getString("token");
        s.username = rs.getString("username");
        s.createdAt = rs.getTimestamp("created_at");
        return s;
    };
    
    public Session findByToken(String token) {
        List<Session> list = jdbc().query(
            "SELECT * FROM sessions WHERE token = ?", ROW_MAPPER, token);
        return list.isEmpty() ? null : list.get(0);
    }
    
    public String findUsernameByToken(String token) {
        Session s = findByToken(token);
        return s != null ? s.username : null;
    }
    
    public void create(String token, String username) {
        jdbc().update(
            "INSERT INTO sessions (token, username) VALUES (?, ?)",
            token, username);
    }
    
    public void deleteByToken(String token) {
        jdbc().update("DELETE FROM sessions WHERE token = ?", token);
    }
}
