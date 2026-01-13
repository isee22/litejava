package game.werewolf.vo;

import java.util.Map;

/**
 * 投票结果
 */
public class WwVoteResultVO {
    /** 投票记录 [voterSeat] -> targetSeat */
    public Map<Integer, Integer> votes;
    /** 被投出的座位 (-1表示平票) */
    public int eliminatedSeat;
}
