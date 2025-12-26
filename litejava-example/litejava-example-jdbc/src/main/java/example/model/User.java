package example.model;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * 用户模型
 */
@Table(name = "users")
public class User {
    public Long id;
    public String username;
    public String password;
    
    @Column(name = "created_at")
    public Timestamp createdAt;
}
