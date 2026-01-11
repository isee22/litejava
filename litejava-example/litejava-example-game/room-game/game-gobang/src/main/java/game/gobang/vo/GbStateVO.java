package game.gobang.vo;

/**
 * 五子棋游戏状态 (断线重连)
 */
public class GbStateVO {
    public int[][] board;
    public long currentPlayer;
    public boolean isOver;
    public int winner;
}
