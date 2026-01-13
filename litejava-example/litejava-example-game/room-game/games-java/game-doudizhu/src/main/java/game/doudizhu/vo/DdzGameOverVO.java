package game.doudizhu.vo;

/**
 * 斗地主游戏结束
 */
public class DdzGameOverVO {
    /** 获胜座位 */
    public int winner;
    /** 地主是否获胜 */
    public boolean landlordWin;
    /** 各玩家得分 */
    public int[] scores;
}
