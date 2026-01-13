package game.robot.ai;

import game.robot.Cmd;
import game.robot.Robot;

import java.util.*;
import java.util.concurrent.*;

/**
 * 五子棋 AI
 */
public class GobangAI extends GameAI {
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final Map<Long, int[][]> boards = new ConcurrentHashMap<>();
    private final Map<Long, Integer> robotSeats = new ConcurrentHashMap<>();
    
    @Override
    protected void onSeatAssigned(Robot robot, int seatIndex) {
        robotSeats.put(robot.userId, seatIndex);
    }
    
    @Override
    public void onGameStart(Robot robot, Map<String, Object> data) {
        boards.put(robot.userId, new int[15][15]);
    }
    
    @Override
    public void onGameCmd(Robot robot, int cmd, int code, Map<String, Object> data) {
        switch (cmd) {
            case Cmd.USER_READY:
                if (data != null && data.containsKey("userId") && data.containsKey("seatIndex")) {
                    long uid = ((Number) data.get("userId")).longValue();
                    if (uid == robot.userId) {
                        robotSeats.put(robot.userId, ((Number) data.get("seatIndex")).intValue());
                    }
                }
                break;
                
            case Cmd.TURN:
                int turnSeat = ((Number) data.get("seatIndex")).intValue();
                Integer mySeat = robotSeats.get(robot.userId);
                if (mySeat != null && mySeat == turnSeat) {
                    scheduler.schedule(() -> {
                        int[] pos = findBestMove(robot.userId);
                        Map<String, Object> moveData = new HashMap<>();
                        moveData.put("x", pos[0]);
                        moveData.put("y", pos[1]);
                        robot.sendGameCmd(Cmd.GB_MOVE, moveData);
                    }, 500, TimeUnit.MILLISECONDS);
                }
                break;
                
            case Cmd.GB_MOVE_RESULT:
                int x = ((Number) data.get("x")).intValue();
                int y = ((Number) data.get("y")).intValue();
                int color = ((Number) data.get("color")).intValue();
                
                int[][] board = boards.get(robot.userId);
                if (board != null) {
                    board[x][y] = color;
                }
                break;
        }
    }
    
    private int[] findBestMove(long robotId) {
        int[][] board = boards.get(robotId);
        if (board == null) {
            return new int[]{7, 7};
        }
        
        int bestScore = -1;
        int bestX = 7, bestY = 7;
        
        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 15; y++) {
                if (board[x][y] != 0) continue;
                
                int score = evaluatePosition(board, x, y);
                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        
        return new int[]{bestX, bestY};
    }
    
    private int evaluatePosition(int[][] board, int x, int y) {
        int score = 0;
        
        int[][] dirs = {{1,0}, {0,1}, {1,1}, {1,-1}};
        for (int[] dir : dirs) {
            int count = 0;
            for (int i = -4; i <= 4; i++) {
                int nx = x + dir[0] * i;
                int ny = y + dir[1] * i;
                if (nx >= 0 && nx < 15 && ny >= 0 && ny < 15 && board[nx][ny] != 0) {
                    count++;
                }
            }
            score += count * 10;
        }
        
        int centerDist = Math.abs(x - 7) + Math.abs(y - 7);
        score += (14 - centerDist);
        
        return score;
    }
    
    @Override
    public void onGameOver(Robot robot, Map<String, Object> data) {
        boards.remove(robot.userId);
        robotSeats.remove(robot.userId);
    }
}
