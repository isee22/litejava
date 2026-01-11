package game.gobang.vo;

/**
 * 落子结果
 */
public class GbMoveVO {
    public long userId;
    public int row;
    public int col;
    public int stone;  // 1=黑, 2=白
}
