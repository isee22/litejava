package game.robot.ai;

import game.robot.Cmd;
import game.robot.Robot;

import java.util.*;
import java.util.concurrent.*;

/**
 * 德州扑克 AI
 */
public class TexasAI extends GameAI {
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final Map<Long, int[]> holeCards = new ConcurrentHashMap<>();
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
                List<Number> cards = (List<Number>) data.get("cards");
                holeCards.put(robot.userId, new int[]{cards.get(0).intValue(), cards.get(1).intValue()});
                break;
                
            case Cmd.TURN:
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
                        if (actionCmd == Cmd.TX_RAISE) {
                            actionData.put("amount", currentBet * 2);
                        } else if (actionCmd == Cmd.TX_CALL) {
                            actionData.put("amount", currentBet - myBet);
                        }
                        robot.sendGameCmd(actionCmd, actionData.isEmpty() ? null : actionData);
                    }, 500, TimeUnit.MILLISECONDS);
                }
                break;
        }
    }
    
    private int decideAction(long robotId, int currentBet, int myBet, int myChips, int pot) {
        int[] hole = holeCards.get(robotId);
        if (hole == null) {
            return Cmd.TX_FOLD;
        }
        
        int handStrength = evaluateHand(hole);
        int toCall = currentBet - myBet;
        
        if (handStrength >= 80) {
            return Cmd.TX_RAISE;
        } else if (handStrength >= 50) {
            if (toCall <= myChips / 4) {
                return Cmd.TX_CALL;
            }
            return Cmd.TX_FOLD;
        } else if (handStrength >= 30) {
            if (toCall <= myChips / 10) {
                return Cmd.TX_CALL;
            }
            return Cmd.TX_FOLD;
        } else {
            if (toCall == 0) {
                return Cmd.TX_CHECK;
            }
            return Cmd.TX_FOLD;
        }
    }
    
    private int evaluateHand(int[] hole) {
        int v1 = hole[0] % 13;
        int v2 = hole[1] % 13;
        int s1 = hole[0] / 13;
        int s2 = hole[1] / 13;
        
        int score = 0;
        
        if (v1 == v2) {
            score = 50 + v1 * 3;
        } else {
            int high = Math.max(v1, v2);
            score = high * 3;
            
            if (s1 == s2) {
                score += 10;
            }
            
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
