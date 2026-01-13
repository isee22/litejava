package game.robot.ai;

import game.robot.Cmd;
import game.robot.Robot;

import java.util.*;
import java.util.concurrent.*;

/**
 * 牛牛 AI
 */
public class NiuniuAI extends GameAI {
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final Map<Long, Integer> robotSeats = new ConcurrentHashMap<>();
    
    @Override
    protected void onSeatAssigned(Robot robot, int seatIndex) {
        robotSeats.put(robot.userId, seatIndex);
    }
    
    @Override
    public void onGameStart(Robot robot, Map<String, Object> data) {
    }
    
    @Override
    @SuppressWarnings("unchecked")
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
                
            case Cmd.DEAL:
                scheduler.schedule(() -> {
                    robot.sendGameCmd(Cmd.NN_SHOW, null);
                }, 500, TimeUnit.MILLISECONDS);
                break;
                
            case Cmd.NN_BET_RESULT:
                if (data != null && data.containsKey("nextSeat")) {
                    int nextSeat = ((Number) data.get("nextSeat")).intValue();
                    Integer mySeat = robotSeats.get(robot.userId);
                    if (mySeat != null && mySeat == nextSeat) {
                        int maxBet = data.containsKey("maxBet") 
                            ? ((Number) data.get("maxBet")).intValue() : 10;
                        scheduler.schedule(() -> {
                            int bet = new Random().nextInt(maxBet / 2) + 1;
                            Map<String, Object> betData = new HashMap<>();
                            betData.put("amount", bet);
                            robot.sendGameCmd(Cmd.NN_BET, betData);
                        }, 300, TimeUnit.MILLISECONDS);
                    }
                }
                break;
        }
    }
    
    @Override
    public void onGameOver(Robot robot, Map<String, Object> data) {
        robotSeats.remove(robot.userId);
    }
}
