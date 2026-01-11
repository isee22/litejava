package game.robot.ai;

import game.common.Cmd;
import game.mahjong.MjCmd;
import game.robot.Robot;

import java.util.*;
import java.util.concurrent.*;

/**
 * 麻将 AI
 */
public class MahjongAI extends GameAI {
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 机器人手牌
    private final Map<Long, List<Integer>> robotTiles = new ConcurrentHashMap<>();
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
                // 收到手牌
                List<Number> tiles = (List<Number>) data.get("tiles");
                List<Integer> tileList = new ArrayList<>();
                for (Number n : tiles) tileList.add(n.intValue());
                robotTiles.put(robot.userId, tileList);
                break;
                
            case Cmd.DRAW:
                // 摸牌
                int tile = ((Number) data.get("tile")).intValue();
                List<Integer> myTiles = robotTiles.get(robot.userId);
                if (myTiles != null) {
                    myTiles.add(tile);
                }
                break;
                
            case Cmd.TURN:
                // 轮到出牌
                int turnSeat = ((Number) data.get("seatIndex")).intValue();
                Integer mySeat = robotSeats.get(robot.userId);
                if (mySeat != null && mySeat == turnSeat) {
                    scheduler.schedule(() -> {
                        int discardTile = decideDiscard(robot.userId);
                        Map<String, Object> discardData = new HashMap<>();
                        discardData.put("tile", discardTile);
                        robot.sendGameCmd(MjCmd.DISCARD, discardData);
                    }, 500, TimeUnit.MILLISECONDS);
                }
                break;
                
            case MjCmd.ACTION:
                // 碰/杠/胡提示
                List<String> actions = (List<String>) data.get("actions");
                
                scheduler.schedule(() -> {
                    String action = decideAction(robot.userId, actions);
                    int actionCmd = getActionCmd(action);
                    robot.sendGameCmd(actionCmd, null);
                }, 300, TimeUnit.MILLISECONDS);
                break;
                
            case MjCmd.DISCARD_RESULT:
                // 出牌结果
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
            case "peng": return MjCmd.PENG;
            case "gang": return MjCmd.GANG;
            case "hu": return MjCmd.HU;
            default: return MjCmd.PASS;
        }
    }
    
    /**
     * 决定出哪张牌
     */
    private int decideDiscard(long robotId) {
        List<Integer> tiles = robotTiles.get(robotId);
        if (tiles == null || tiles.isEmpty()) {
            return 0;
        }
        
        // 简单策略：出孤张
        Map<Integer, Integer> counts = new HashMap<>();
        for (int tile : tiles) {
            counts.merge(tile, 1, Integer::sum);
        }
        
        // 找单张
        for (int tile : tiles) {
            if (counts.get(tile) == 1) {
                return tile;
            }
        }
        
        // 没有单张，出最后一张
        return tiles.get(tiles.size() - 1);
    }
    
    /**
     * 决定动作
     */
    private String decideAction(long robotId, List<String> actions) {
        // 优先级：胡 > 杠 > 碰 > 过
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
