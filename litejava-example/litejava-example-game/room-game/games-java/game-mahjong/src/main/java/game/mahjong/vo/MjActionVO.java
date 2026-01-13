package game.mahjong.vo;

import java.util.List;

/**
 * 玩家可执行的操作
 */
public class MjActionVO {
    /** 玩家座位 */
    public int seatIndex;
    /** 可执行的操作列表: peng, gang, hu */
    public List<String> actions;
}
