package game.account.entity;

/**
 * 道具配置表
 */
public class ItemConfigEntity {
    public int itemId;
    public String name;
    public String type;       // coin_card, exp_card, gift, emoji
    public String desc;
    public int price;         // 钻石价格
    public int coinPrice;     // 金币价格 (0表示不能用金币买)
    public int duration;      // 持续时间(秒), 0表示永久/一次性
    public String effect;     // JSON格式效果参数
    public int enabled;       // 是否启用
    public int sortOrder;     // 排序
}
