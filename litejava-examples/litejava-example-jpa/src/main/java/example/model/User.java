package example.model;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * 用户实体
 */
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    public String username;
    public String password;
    
    @Column(name = "created_at")
    public Timestamp createdAt;
}
