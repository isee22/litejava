package game.account.entity;

import javax.persistence.*;

/**
 * 签到奖励配置
 */
@Entity
@Table(name = "sign_in_config")
public class SignInConfig {
    @Id
    @Column(name = "day")
    public int day;           // 第几天 (1-7)
    
    @Column(name = "coins")
    public int coins;         // 金币奖励
    
    @Column(name = "diamonds")
    public int diamonds;      // 钻石奖励
    
    @Column(name = "itemIds")
    public String itemIds;    // 道具奖励 (逗号分隔的道具ID:数量)
    
    @Column(name = "description")
    public String description;  // 描述
}
