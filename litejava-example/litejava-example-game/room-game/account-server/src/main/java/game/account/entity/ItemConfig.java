package game.account.entity;

import javax.persistence.*;

/**
 * 道具配置表
 */
@Entity
@Table(name = "item_config")
public class ItemConfig {
    @Id
    @Column(name = "itemId")
    public int itemId;
    
    @Column(name = "name")
    public String name;
    
    @Column(name = "type")
    public String type;       // coin_card, exp_card, gift, emoji
    
    @Column(name = "desc")
    public String desc;
    
    @Column(name = "price")
    public int price;         // 钻石价格
    
    @Column(name = "coinPrice")
    public int coinPrice;     // 金币价格 (0表示不能用金币买)
    
    @Column(name = "duration")
    public int duration;      // 持续时间(秒), 0表示永久/一次性
    
    @Column(name = "effect")
    public String effect;     // JSON格式效果参数
    
    @Column(name = "enabled")
    public int enabled;       // 是否启用
    
    @Column(name = "sortOrder")
    public int sortOrder;     // 排序
}
