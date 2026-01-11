package game.robot.ai;

import game.common.Cmd;
import game.niuniu.NnCmd;
import game.robot.Robot;

import java.util.*;
import java.util.concurrent.*;

/**
 * 牛牛 AI
 */
public class NiuniuAI extends GameAI {
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 机器人座位
    private final Map<Long, Integer> robotSeats = new ConcurrentHashMap<>();
    
    @Override
    protected void onSeatAssigned(Robot robot, int seatIndex) {
        robotSeats.put(robot.userId, seatIndex);
    }
    
    @Override
    public void onGameStart(Robot robot, Map<String, Object> data) {
        // 等待发牌
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void onGameCmd(Robot robot, int cmd, int code, Map<String, Object> data) {
        switch (cmd) {
            case Cmd.USER_READY:
                // 从准备消息中获取座位
                if (data != null && data.containsKey("userId") && data.containsKey("seatIndex")) {
                    long uid = ((Number) data.get("userId")).longValue();
                    if (uid == robot.userId) {
                        robotSeats.put(robot.userId, ((Number) data.get("seatIndex")).intValue());
                    }
                }
                break;
                
            case Cmd.DEAL:
                // 收到手牌，自动亮牌
                scheduler.schedule(() -> {
                    robot.sendGameCmd(NnCmd.SHOW, null);
                }, 500, TimeUnit.MILLISECONDS);
                break;
                
            case NnCmd.BET_RESULT:
                // 下注阶段，检查是否需要下注
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
                            robot.sendGameCmd(NnCmd.BET, betData);
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
