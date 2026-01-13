package game.doudizhu.vo;

import java.util.List;

/**
 * 斗地主游戏状态 (断线重连)
 */
public class DdzStateVO {
    public int status;
    public int currentSeat;
    public int landlordSeat;
    public List<Integer> myCards;
    public int[] lastCards;
    public int lastPlaySeat;
    public int[] bottomCards;
}
