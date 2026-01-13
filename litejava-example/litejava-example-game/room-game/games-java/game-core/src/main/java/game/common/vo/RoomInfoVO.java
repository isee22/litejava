package game.common.vo;

import java.util.List;

/**
 * 房间信息
 */
public class RoomInfoVO {
    public String roomId;
    public long ownerId;
    public int playerCount;
    public int maxPlayers;
    public boolean gaming;
    public List<SeatVO> seats;
}
