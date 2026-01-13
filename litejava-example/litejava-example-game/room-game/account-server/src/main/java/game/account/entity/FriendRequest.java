package game.account.entity;

import javax.persistence.*;

/**
 * 好友请求实体
 * 
 * status: 0=待处理, 1=已接受, 2=已拒绝
 */
@Entity
@Table(name = "friend_request")
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    
    @Column(name = "fromId")
    public long fromId;
    
    @Column(name = "toId")
    public long toId;
    
    @Column(name = "message")
    public String message;
    
    @Column(name = "status")
    public int status;
    
    @Column(name = "createTime")
    public long createTime;
    
    @Column(name = "updateTime")
    public long updateTime;
}
