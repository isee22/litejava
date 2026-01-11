package game.account.entity;

/**
 * 签到奖励配置
 */
public class SignInConfigEntity {
    public int day;           // 第几天 (1-7)
    public int coins;         // 金币奖励
    public int diamonds;      // 钻石奖励
    public String itemIds;    // 道具奖励 (逗号分隔的道具ID:数量)
    public String desc;       // 描述
}
