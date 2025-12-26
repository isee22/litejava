package example.model;

import java.sql.Timestamp;

/**
 * 会话模型
 */
public class Session {
    public Long id;
    public String token;
    public String username;
    public Timestamp createdAt;
}
