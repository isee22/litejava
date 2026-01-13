package game.robot.ai;

import game.robot.Cmd;
import game.robot.Robot;

import java.util.*;
import java.util.concurrent.*;

/**
 * 麻将 AI
 */
public class MahjongAI extends GameAI {
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final Map<Long, List<Integer>> robotTiles = new ConcurrentHashMap<>();
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
                List<Number> tiles = (List<Number>) data.get("tiles");
                List<Integer> tileList = new ArrayList<>();
                for (Number n : tiles) tileList.add(n.intValue());
                robotTiles.put(robot.userId, tileList);
                break;
                
            case Cmd.DRAW:
                int tile = ((Number) data.get("tile")).intValue();
                List<Integer> myTiles = robotTiles.get(robot.userId);
                if (myTiles != null) {
                    myTiles.add(tile);
                }
                break;
                
            case Cmd.TURN:
                int turnSeat = ((Number) data.get("seatIndex")).intValue();
                Integer mySeat = robotSeats.get(robot.userId);
                if (mySeat != null && mySeat == turnSeat) {
                    scheduler.schedule(() -> {
                        int discardTile = decideDiscard(robot.userId);
                        Map<String, Object> discardData = new HashMap<>();
                        discardData.put("tile", discardTile);
                        robot.sendGameCmd(Cmd.MJ_DISCARD, discardData);
                    }, 500, TimeUnit.MILLISECONDS);
                }
                break;
                
            case Cmd.MJ_ACTION:
                List<String> actions = (List<String>) data.get("actions");
                scheduler.schedule(() -> {
                    String action = decideAction(robot.userId, actions);
                    int actionCmd = getActionCmd(action);
                    robot.sendGameCmd(actionCmd, null);
                }, 300, TimeUnit.MILLISECONDS);
                break;
                
            case Cmd.MJ_DISCARD_RESULT:
                long playerId = ((Number) data.get("userId")).longValue();
                if (playerId == robot.userId) {
                    int discarded = ((Number) data.get("tile")).intValue();
                    List<Integer> tiles2 = robotTiles.get(robot.userId);
                    if (tiles2 != null) {
                        tiles2.remove(Integer.valueOf(discarded));
                    }
                }
                break;
        }
    }
    
    private int getActionCmd(String action) {
        switch (action) {
            case "peng": return Cmd.MJ_PENG;
            case "gang": return Cmd.MJ_GANG;
            case "hu": return Cmd.MJ_HU;
            default: return Cmd.MJ_PASS;
        }
    }
    
    private int decideDiscard(long robotId) {
        List<Integer> tiles = robotTiles.get(robotId);
        if (tiles == null || tiles.isEmpty()) {
            return 0;
        }
        
        Map<Integer, Integer> counts = new HashMap<>();
        for (int t : tiles) {
            counts.merge(t, 1, Integer::sum);
        }
        
        for (int t : tiles) {
            if (counts.get(t) == 1) {
                return t;
            }
        }
        
        return tiles.get(tiles.size() - 1);
    }
    
    private String decideAction(long robotId, List<String> actions) {
        if (actions.contains("hu")) return "hu";
        if (actions.contains("gang")) return "gang";
        if (actions.contains("peng")) return "peng";
        return "pass";
    }
    
    @Override
    public void onGameOver(Robot robot, Map<String, Object> data) {
        robotTiles.remove(robot.userId);
        robotSeats.remove(robot.userId);
    }
}
