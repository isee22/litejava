package game.robot.ai;

import game.robot.Robot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏 AI 基类
 */
public abstract class GameAI {
    
    private static final Map<String, GameAI> AI_MAP = new HashMap<>();
    
    public static void register(String gameType, GameAI ai) {
        AI_MAP.put(gameType, ai);
    }
    
    public static GameAI get(String gameType) {
        return AI_MAP.get(gameType);
    }
    
    /**
     * 进入房间 - 从 seats 中获取自己的座位
     */
    @SuppressWarnings("unchecked")
    public void onRoomJoin(Robot robot, Map<String, Object> data) {
        List<Map<String, Object>> seats = (List<Map<String, Object>>) data.get("seats");
        if (seats == null) return;
        
        for (Map<String, Object> seat : seats) {
            long uid = ((Number) seat.get("userId")).longValue();
            if (uid == robot.userId) {
                int seatIndex = ((Number) seat.get("seatIndex")).intValue();
                onSeatAssigned(robot, seatIndex);
                break;
            }
        }
    }
    
    /**
     * 座位分配 - 子类可重写
     */
    protected void onSeatAssigned(Robot robot, int seatIndex) {
        // 默认空实现，子类重写
    }
    
    /**
     * 游戏开始
     */
    public abstract void onGameStart(Robot robot, Map<String, Object> data);
    
    /**
     * 游戏命令
     */
    public abstract void onGameCmd(Robot robot, int cmd, int code, Map<String, Object> data);
    
    /**
     * 游戏结束
     */
    public void onGameOver(Robot robot, Map<String, Object> data) {
        // 默认空实现
    }
}
