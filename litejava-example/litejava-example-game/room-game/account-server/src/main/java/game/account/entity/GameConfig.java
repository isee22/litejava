package game.account.entity;

import javax.persistence.*;

/**
 * 房间配置实体 (场次配置)
 * 
 * 对应表: room_config
 * 
 * 每个游戏可以有多个场次，如：斗地主初级场、斗地主高级场
 */
@Entity
@Table(name = "room_config")
public class GameConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    
    @Column(name = "gameType")
    public String gameType;       // 游戏类型: doudizhu, mahjong, texas, niuniu, gobang, moba
    
    @Column(name = "roomLevel")
    public int roomLevel;         // 场次级别: 1=初级, 2=中级, 3=高级, 4=大师
    
    @Column(name = "roomName")
    public String roomName;       // 场次名称: 初级场、中级场、高级场、大师场
    
    @Column(name = "minPlayers")
    public int minPlayers;        // 最小玩家数
    
    @Column(name = "maxPlayers")
    public int maxPlayers;        // 最大玩家数
    
    @Column(name = "minCoins")
    public int minCoins;          // 入场最低金币
    
    @Column(name = "maxCoins")
    public int maxCoins;          // 入场最高金币 (0=不限)
    
    @Column(name = "baseScore")
    public int baseScore;         // 底分
    
    @Column(name = "initChips")
    public int initChips;         // 初始筹码 (德州等)
    
    @Column(name = "roundTime")
    public int roundTime;         // 每回合时间(秒)
    
    @Column(name = "matchTimeout")
    public int matchTimeout;      // 匹配超时(秒)
    
    @Column(name = "extraConfig")
    public String extraConfig;    // 扩展配置(JSON)
    
    @Column(name = "status")
    public int status;            // 状态: 0=关闭, 1=开启
    
    @Column(name = "sortOrder")
    public int sortOrder;         // 排序
    
    @Column(name = "createTime")
    public long createTime;
    
    @Column(name = "updateTime")
    public long updateTime;
}
