package game.robot.ai;

import game.common.Cmd;
import game.texas.TxCmd;
import game.robot.Robot;

import java.util.*;
import java.util.concurrent.*;

/**
 * 德州扑克 AI
 */
public class TexasAI extends GameAI {
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 机器人底牌
    private final Map<Long, int[]> holeCards = new ConcurrentHashMap<>();
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
                // 收到底牌
                List<Number> cards = (List<Number>) data.get("cards");
                holeCards.put(robot.userId, new int[]{cards.get(0).intValue(), cards.get(1).intValue()});
                break;
                
            case Cmd.TURN:
                // 轮到操作
                int turnSeat = ((Number) data.get("seatIndex")).intValue();
                Integer mySeat = robotSeats.get(robot.userId);
                if (mySeat != null && mySeat == turnSeat) {
                    int currentBet = data.containsKey("currentBet") 
                        ? ((Number) data.get("currentBet")).intValue() : 0;
                    int myBet = data.containsKey("myBet") 
                        ? ((Number) data.get("myBet")).intValue() : 0;
                    int myChips = data.containsKey("myChips") 
                        ? ((Number) data.get("myChips")).intValue() : 1000;
                    int pot = data.containsKey("pot") 
                        ? ((Number) data.get("pot")).intValue() : 0;
                    
                    scheduler.schedule(() -> {
                        int actionCmd = decideAction(robot.userId, currentBet, myBet, myChips, pot);
                        Map<String, Object> actionData = new HashMap<>();
                        if (actionCmd == TxCmd.RAISE) {
                            actionData.put("amount", currentBet * 2);
                        } else if (actionCmd == TxCmd.CALL) {
                            actionData.put("amount", currentBet - myBet);
                        }
                        robot.sendGameCmd(actionCmd, actionData.isEmpty() ? null : actionData);
                    }, 500, TimeUnit.MILLISECONDS);
                }
                break;
        }
    }
    
    /**
     * 决定操作
     */
    private int decideAction(long robotId, int currentBet, int myBet, int myChips, int pot) {
        int[] hole = holeCards.get(robotId);
        if (hole == null) {
            return TxCmd.FOLD;
        }
        
        int handStrength = evaluateHand(hole);
        int toCall = currentBet - myBet;
        
        // 简单策略
        if (handStrength >= 80) {
            return TxCmd.RAISE;
        } else if (handStrength >= 50) {
            if (toCall <= myChips / 4) {
                return TxCmd.CALL;
            }
            return TxCmd.FOLD;
        } else if (handStrength >= 30) {
            if (toCall <= myChips / 10) {
                return TxCmd.CALL;
            }
            return TxCmd.FOLD;
        } else {
            if (toCall == 0) {
                return TxCmd.CHECK;
            }
            return TxCmd.FOLD;
        }
    }
    
    /**
     * 评估手牌强度 (0-100)
     */
    private int evaluateHand(int[] hole) {
        int v1 = hole[0] % 13;
        int v2 = hole[1] % 13;
        int s1 = hole[0] / 13;
        int s2 = hole[1] / 13;
        
        int score = 0;
        
        // 对子
        if (v1 == v2) {
            score = 50 + v1 * 3;
        } else {
            // 高牌
            int high = Math.max(v1, v2);
            score = high * 3;
            
            // 同花
            if (s1 == s2) {
                score += 10;
            }
            
            // 连牌
            if (Math.abs(v1 - v2) == 1) {
                score += 8;
            }
        }
        
        return Math.min(100, score);
    }
    
    @Override
    public void onGameOver(Robot robot, Map<String, Object> data) {
        holeCards.remove(robot.userId);
        robotSeats.remove(robot.userId);
    }
}
