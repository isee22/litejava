package game.account.entity;

/**
 * 房间配置实体 (场次配置)
 * 
 * 对应表: room_config
 * 
 * 每个游戏可以有多个场次，如：斗地主初级场、斗地主高级场
 */
public class GameConfigEntity {
    
    public int id;                // 主键
    public String gameType;       // 游戏类型: doudizhu, mahjong, texas, niuniu, gobang, moba
    public String roomLevel;      // 场次级别: beginner, intermediate, advanced, master
    public String roomName;       // 场次名称: 初级场、中级场、高级场、大师场
    public int minPlayers;        // 最小玩家数
    public int maxPlayers;        // 最大玩家数
    public int minCoins;          // 入场最低金币
    public int maxCoins;          // 入场最高金币 (0=不限)
    public int baseScore;         // 底分
    public int initChips;         // 初始筹码 (德州等)
    public int roundTime;         // 每回合时间(秒)
    public int matchTimeout;      // 匹配超时(秒)
    public String extraConfig;    // 扩展配置(JSON)
    public int status;            // 状态: 0=关闭, 1=开启
    public int sortOrder;         // 排序
    public long createTime;
    public long updateTime;
}
