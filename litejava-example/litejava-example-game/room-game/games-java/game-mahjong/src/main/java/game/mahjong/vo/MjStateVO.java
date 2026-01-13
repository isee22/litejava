package game.mahjong.vo;

import java.util.List;

public class MjStateVO {
    public int status;
    public int currentSeat;
    public int dealerSeat;
    public List<Integer> myHand;
    public int remaining;
    public int lastDiscard;
    public int lastDiscardSeat;
}
