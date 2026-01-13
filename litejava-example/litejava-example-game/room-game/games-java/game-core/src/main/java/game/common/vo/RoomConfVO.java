package game.common.vo;

/**
 * 房间配置
 */
public class RoomConfVO {
    public int players;
    public String gameType;
    public int roomLevel;
    public int baseScore;
    
    /** 操作超时时间 (秒)，默认 15 秒 */
    public int turnTimeout = 15;
    
    /** 叫地主超时时间 (秒)，默认 10 秒 */
    public int bidTimeout = 10;
}
