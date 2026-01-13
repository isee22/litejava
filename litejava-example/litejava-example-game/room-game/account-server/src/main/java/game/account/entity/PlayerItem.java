package game.account.entity;

import javax.persistence.*;

/**
 * 玩家道具表
 */
@Entity
@Table(name = "player_item")
public class PlayerItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    
    @Column(name = "userId")
    public long userId;
    
    @Column(name = "itemId")
    public int itemId;
    
    @Column(name = "count")
    public int count;
    
    @Column(name = "expireTime")
    public long expireTime;   // 过期时间戳, 0表示永久
    
    @Column(name = "createTime")
    public long createTime;
    
    @Column(name = "updateTime")
    public long updateTime;
}
