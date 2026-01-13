package game.doudizhu.vo;

import java.util.List;

/**
 * 叫地主结果 (Game层返回)
 */
public class DdzBidVO {
    /** 叫地主的座位 */
    public int seat;
    /** 是否叫地主 */
    public boolean bid;
    /** 下一个叫地主的座位 (-1表示叫地主阶段结束) */
    public int nextBidSeat = -1;
    /** 地主座位 (确定地主时) */
    public int landlordSeat = -1;
    /** 底牌 (确定地主时) */
    public int[] bottomCards;
    /** 当前操作座位 (确定地主后) */
    public int currentSeat = -1;
    /** 是否重新发牌 */
    public boolean redeal;
}
