package game.doudizhu.vo;

import java.util.List;

/**
 * 叫地主结果
 */
public class DdzBidResultVO {
    public int seatIndex;
    public boolean bid;
    public int nextSeat;
    public int landlordSeat = -1;
    public List<Integer> bottomCards;
    /** 是否重新发牌 (无人叫地主) */
    public boolean redeal;
}
