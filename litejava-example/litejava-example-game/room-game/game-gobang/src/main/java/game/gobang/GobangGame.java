package game.gobang;

import java.util.*;

/**
 * 五子棋游戏逻辑
 */
public class GobangGame {
    
    public static final int BOARD_SIZE = 15;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;
    
    // 棋盘 [row][col]
    public int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    
    // 玩家 userId
    public long[] players = new long[2];
    
    // 当前回合 (0=黑, 1=白)
    public int currentTurn = 0;
    
    // 游戏状态
    public boolean isOver = false;
    public int winner = -1; // -1=未结束, 0=黑胜, 1=白胜, 2=平局
    
    // 落子历史
    public List<int[]> history = new ArrayList<>();
    
    public GobangGame(long player1, long player2) {
        this.players[0] = player1; // 黑
        this.players[1] = player2; // 白
    }
    
    /**
     * 落子
     * @return 0=成功, 1=不是你的回合, 2=位置无效, 3=位置已有棋子
     */
    public int move(long userId, int row, int col) {
        int playerIndex = getPlayerIndex(userId);
        if (playerIndex < 0) {
            return 1;
        }
        if (playerIndex != currentTurn) {
            return 1;
        }
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return 2;
        }
        if (board[row][col] != EMPTY) {
            return 3;
        }
        
        // 落子
        int stone = playerIndex == 0 ? BLACK : WHITE;
        board[row][col] = stone;
        history.add(new int[]{row, col, stone});
        
        // 检查胜负
        if (checkWin(row, col, stone)) {
            isOver = true;
            winner = playerIndex;
        } else if (history.size() >= BOARD_SIZE * BOARD_SIZE) {
            isOver = true;
            winner = 2; // 平局
        } else {
            currentTurn = 1 - currentTurn;
        }
        
        return 0;
    }
    
    /**
     * 检查是否五连
     */
    private boolean checkWin(int row, int col, int stone) {
        // 四个方向: 横、竖、斜、反斜
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int count = 1;
            
            // 正向
            int r = row + dir[0];
            int c = col + dir[1];
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == stone) {
                count++;
                r += dir[0];
                c += dir[1];
            }
            
            // 反向
            r = row - dir[0];
            c = col - dir[1];
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == stone) {
                count++;
                r -= dir[0];
                c -= dir[1];
            }
            
            if (count >= 5) {
                return true;
            }
        }
        
        return false;
    }
    
    public int getPlayerIndex(long userId) {
        if (players[0] == userId) {
            return 0;
        }
        if (players[1] == userId) {
            return 1;
        }
        return -1;
    }
    
    public long getCurrentPlayer() {
        return players[currentTurn];
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("board", board);
        map.put("currentTurn", currentTurn);
        map.put("currentPlayer", getCurrentPlayer());
        map.put("isOver", isOver);
        map.put("winner", winner);
        map.put("history", history);
        return map;
    }
}
