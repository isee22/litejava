package game.account.entity;

import javax.persistence.*;

/**
 * 账号实体
 */
@Entity
@Table(name = "account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    
    @Column(name = "username")
    public String username;
    
    @Column(name = "password")
    public String password;
    
    @Column(name = "createTime")
    public long createTime;
    
    @Column(name = "lastLoginTime")
    public long lastLoginTime;
}
