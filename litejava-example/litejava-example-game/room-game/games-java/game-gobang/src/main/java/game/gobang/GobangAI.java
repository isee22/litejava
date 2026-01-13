package game.gobang;

/**
 * 五子棋 AI
 * 
 * 策略：评分法，计算每个空位的攻防分数
 */
public class GobangAI {
    
    private static final int SIZE = 15;
    
    // 棋型分数
    private static final int FIVE = 100000;      // 连五
    private static final int LIVE_FOUR = 10000;  // 活四
    private static final int RUSH_FOUR = 1000;   // 冲四
    private static final int LIVE_THREE = 1000;  // 活三
    private static final int SLEEP_THREE = 100;  // 眠三
    private static final int LIVE_TWO = 100;     // 活二
    private static final int SLEEP_TWO = 10;     // 眠二
    
    /**
     * 计算最佳落子位置
     * @param board 棋盘 (0=空, 1=黑, 2=白)
     * @param myStone 我方棋子 (1 或 2)
     * @return [row, col]
     */
    public static int[] findBestMove(int[][] board, int myStone) {
        int bestRow = -1, bestCol = -1;
        int bestScore = Integer.MIN_VALUE;
        
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != 0) continue;
                if (!hasNeighbor(board, r, c)) continue;
                
                int score = evaluatePosition(board, r, c, myStone);
                if (score > bestScore) {
                    bestScore = score;
                    bestRow = r;
                    bestCol = c;
                }
            }
        }
        
        // 如果没找到，下中心
        if (bestRow < 0) {
            bestRow = SIZE / 2;
            bestCol = SIZE / 2;
        }
        
        return new int[]{bestRow, bestCol};
    }
    
    /**
     * 检查周围是否有棋子
     */
    private static boolean hasNeighbor(int[][] board, int row, int col) {
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr, nc = col + dc;
                if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                    if (board[nr][nc] != 0) return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 评估某位置的分数
     */
    private static int evaluatePosition(int[][] board, int row, int col, int myStone) {
        int oppStone = 3 - myStone;
        
        // 进攻分 + 防守分
        int attackScore = calcScore(board, row, col, myStone);
        int defendScore = calcScore(board, row, col, oppStone);
        
        return attackScore + (int)(defendScore * 0.9);
    }
    
    /**
     * 计算某位置对某方的分数
     */
    private static int calcScore(int[][] board, int row, int col, int stone) {
        int score = 0;
        
        // 四个方向：横、竖、斜、反斜
        int[][] dirs = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        
        for (int[] dir : dirs) {
            int count = 1;
            int block = 0;
            int empty = 0;
            
            // 正向
            for (int i = 1; i <= 4; i++) {
                int nr = row + dir[0] * i;
                int nc = col + dir[1] * i;
                if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) {
                    block++;
                    break;
                }
                if (board[nr][nc] == stone) {
                    count++;
                } else if (board[nr][nc] == 0) {
                    empty++;
                    break;
                } else {
                    block++;
                    break;
                }
            }
            
            // 反向
            for (int i = 1; i <= 4; i++) {
                int nr = row - dir[0] * i;
                int nc = col - dir[1] * i;
                if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) {
                    block++;
                    break;
                }
                if (board[nr][nc] == stone) {
                    count++;
                } else if (board[nr][nc] == 0) {
                    empty++;
                    break;
                } else {
                    block++;
                    break;
                }
            }
            
            score += getPatternScore(count, block, empty);
        }
        
        return score;
    }
    
    /**
     * 根据棋型获取分数
     */
    private static int getPatternScore(int count, int block, int empty) {
        if (count >= 5) return FIVE;
        if (block == 0) {
            switch (count) {
                case 4: return LIVE_FOUR;
                case 3: return LIVE_THREE;
                case 2: return LIVE_TWO;
            }
        } else if (block == 1) {
            switch (count) {
                case 4: return RUSH_FOUR;
                case 3: return SLEEP_THREE;
                case 2: return SLEEP_TWO;
            }
        }
        return 0;
    }
}
