package game.common.vo;

import java.util.List;

/**
 * 房间状态 - 进入游戏房间后返回
 */
public class RoomStateVO {
    public String roomId;
    public List<SeatVO> seats;
    public Object game;
}
