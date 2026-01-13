package game.account.vo;

/**
 * 排行榜项 VO
 */
public class RankItemVO {
    public int rank;
    public long userId;
    public String name;
    public int level;
    public int value;
    public int total;  // 用于胜场榜
    public int exp;    // 用于等级榜
    public int charm;  // 用于魅力榜
}

