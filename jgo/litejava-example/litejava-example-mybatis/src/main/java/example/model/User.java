package example.model;

import java.sql.Timestamp;

/**
 * 用户模型
 */
public class User {
    public Long id;
    public String username;
    public String email;
    public String password;
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
