package game.account.entity;

import javax.persistence.*;

/**
 * 好友关系实体
 */
@Entity
@Table(name = "friend")
public class Friend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    
    @Column(name = "userId")
    public long userId;
    
    @Column(name = "friendId")
    public long friendId;
    
    @Column(name = "createTime")
    public long createTime;
}
