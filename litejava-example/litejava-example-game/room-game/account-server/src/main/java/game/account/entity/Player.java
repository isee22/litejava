package game.account.entity;

import javax.persistence.*;

/**
 * 玩家实体
 */
@Entity
@Table(name = "player")
public class Player {
    @Id
    @Column(name = "userId")
    public long userId;
    
    @Column(name = "name")
    public String name;
    
    @Column(name = "avatar")
    public String avatar;
    
    @Column(name = "sex")
    public int sex;        // 0=未知, 1=男, 2=女
    
    @Column(name = "coins")
    public int coins;
    
    @Column(name = "diamonds")
    public int diamonds;
    
    @Column(name = "level")
    public int level;
    
    @Column(name = "exp")
    public int exp;
    
    @Column(name = "vipLevel")
    public int vipLevel;
    
    @Column(name = "vipExp")
    public int vipExp;
    
    @Column(name = "totalGames")
    public int totalGames;
    
    @Column(name = "winGames")
    public int winGames;
    
    @Column(name = "escapeGames")
    public int escapeGames;  // 逃跑次数
    
    @Column(name = "creditScore")
    public int creditScore;  // 信用分 (默认100)
    
    @Column(name = "loginDays")
    public int loginDays;
    
    @Column(name = "createTime")
    public long createTime;
    
    @Column(name = "lastLoginTime")
    public long lastLoginTime;
}
