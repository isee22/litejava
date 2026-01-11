package game.robot;

import game.robot.ai.*;
import litejava.App;
import litejava.plugin.StaticFilePlugin;
import litejava.plugins.LiteJava;

import java.util.*;
import java.util.concurrent.*;

/**
 * 机器人客户端 - 简化版
 * 
 * 功能：
 * 1. 按游戏类型管理机器人数量
 * 2. 机器人全自动：连接 → 匹配 → 游戏 → 循环
 * 3. 可配置思考时间范围
 */
public class RobotClient {
    
    private App app;
    private RobotManager manager;
    
    // 机器人池: gameType -> robots
    private final Map<String, List<Robot>> robotPools = new ConcurrentHashMap<>();
    
    // 配置: gameType -> config
    private final Map<String, GameConfig> gameConfigs = new ConcurrentHashMap<>();
    
    // 机器人ID序列
    private static final long ROBOT_ID_START = 900000;
    private long robotIdSeq = ROBOT_ID_START;
    
    // 调度器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * 游戏配置
     */
    public static class GameConfig {
        public String gameType;
        public int robotCount;        // 机器人数量
        public int thinkTimeMin = 500;   // 最小思考时间(ms)
        public int thinkTimeMax = 2000;  // 最大思考时间(ms)
        public String roomLevel = "beginner";
    }
    
    public void start(App app) {
        this.app = app;
        this.manager = new RobotManager(app);
        
        int httpPort = app.conf.getInt("server", "httpPort", 8900);
        
        // 注册 AI 策略
        GameAI.register("doudizhu", new DoudizhuAI());
        GameAI.register("gobang", new GobangAI());
        GameAI.register("mahjong", new MahjongAI());
        GameAI.register("texas", new TexasAI());
        GameAI.register("niuniu", new NiuniuAI());
        
        // 静态文件服务 (dashboard)
        app.use(new StaticFilePlugin("/", "static"));
        
        // HTTP 管理接口
        setupHttpRoutes();
        
        // 定时检查机器人状态
        scheduler.scheduleAtFixedRate(this::checkRobots, 10, 10, TimeUnit.SECONDS);
        
        app.log.info("=== 机器人客户端 ===");
        app.log.info("管理接口: http://localhost:" + httpPort);
        
        app.run(httpPort);
    }
    
    /**
     * 设置游戏机器人数量
     */
    private void setGameRobots(String gameType, int count, int thinkMin, int thinkMax) {
        GameConfig config = gameConfigs.computeIfAbsent(gameType, k -> {
            GameConfig c = new GameConfig();
            c.gameType = k;
            return c;
        });
        config.robotCount = count;
        config.thinkTimeMin = thinkMin;
        config.thinkTimeMax = thinkMax;
        
        List<Robot> pool = robotPools.computeIfAbsent(gameType, k -> new CopyOnWriteArrayList<>());
        
        int current = pool.size();
        if (count > current) {
            // 增加机器人
            for (int i = 0; i < count - current; i++) {
                Robot robot = createRobot(gameType, config);
                pool.add(robot);
                robot.start();
            }
            app.log.info("[" + gameType + "] 增加机器人: " + current + " -> " + count);
        } else if (count < current) {
            // 减少机器人
            for (int i = current - 1; i >= count; i--) {
                Robot robot = pool.remove(i);
                robot.shutdown();
            }
            app.log.info("[" + gameType + "] 减少机器人: " + current + " -> " + count);
        }
    }
    
    /**
     * 创建机器人
     */
    private Robot createRobot(String gameType, GameConfig config) {
        long robotId = robotIdSeq++;
        String name = "机器人" + (robotId - ROBOT_ID_START + 1);
        return new Robot(app, manager, robotId, name, gameType, config);
    }
    
    /**
     * 检查机器人状态
     */
    private void checkRobots() {
        for (Map.Entry<String, List<Robot>> entry : robotPools.entrySet()) {
            String gameType = entry.getKey();
            List<Robot> pool = entry.getValue();
            GameConfig config = gameConfigs.get(gameType);
            
            int idle = 0, matching = 0, gaming = 0, error = 0;
            for (Robot robot : pool) {
                switch (robot.state) {
                    case IDLE: idle++; break;
                    case MATCHING: matching++; break;
                    case CONNECTING: case IN_ROOM: case GAMING: gaming++; break;
                    case RECONNECTING: error++; break;
                }
            }
            
            if (error > 0 || pool.size() != config.robotCount) {
                app.log.info("[" + gameType + "] 机器人状态: 空闲=" + idle + ", 匹配中=" + matching + ", 游戏中=" + gaming + ", 异常=" + error);
            }
        }
    }
    
    private void setupHttpRoutes() {
        // 获取状态
        app.get("/status", ctx -> {
            Map<String, Object> status = new HashMap<>();
            
            List<Map<String, Object>> games = new ArrayList<>();
            for (Map.Entry<String, GameConfig> entry : gameConfigs.entrySet()) {
                String gameType = entry.getKey();
                GameConfig config = entry.getValue();
                List<Robot> pool = robotPools.getOrDefault(gameType, Collections.emptyList());
                
                int idle = 0, matching = 0, gaming = 0;
                for (Robot robot : pool) {
                    switch (robot.state) {
                        case IDLE: idle++; break;
                        case MATCHING: matching++; break;
                        case CONNECTING: case IN_ROOM: case GAMING: gaming++; break;
                    }
                }
                
                Map<String, Object> game = new HashMap<>();
                game.put("gameType", gameType);
                game.put("robotCount", config.robotCount);
                game.put("thinkTimeMin", config.thinkTimeMin);
                game.put("thinkTimeMax", config.thinkTimeMax);
                game.put("idle", idle);
                game.put("matching", matching);
                game.put("gaming", gaming);
                games.add(game);
            }
            
            status.put("games", games);
            ctx.ok(status);
        });
        
        // 设置游戏机器人
        app.post("/game/:gameType", ctx -> {
            String gameType = ctx.pathParam("gameType");
            Map<String, Object> body = ctx.bindJSON();
            
            int count = ((Number) body.getOrDefault("count", 0)).intValue();
            int thinkMin = ((Number) body.getOrDefault("thinkMin", 500)).intValue();
            int thinkMax = ((Number) body.getOrDefault("thinkMax", 2000)).intValue();
            
            setGameRobots(gameType, count, thinkMin, thinkMax);
            
            GameConfig config = gameConfigs.get(gameType);
            Map<String, Object> result = new HashMap<>();
            result.put("gameType", gameType);
            result.put("robotCount", config.robotCount);
            result.put("thinkTimeMin", config.thinkTimeMin);
            result.put("thinkTimeMax", config.thinkTimeMax);
            ctx.ok(result);
        });
        
        // 删除游戏机器人
        app.delete("/game/:gameType", ctx -> {
            String gameType = ctx.pathParam("gameType");
            setGameRobots(gameType, 0, 500, 2000);
            gameConfigs.remove(gameType);
            ctx.ok(null);
        });
        
        // 健康检查
        app.get("/health", ctx -> {
            int total = 0;
            for (List<Robot> pool : robotPools.values()) {
                total += pool.size();
            }
            Map<String, Object> data = new HashMap<>();
            data.put("status", "UP");
            data.put("totalRobots", total);
            data.put("games", gameConfigs.size());
            ctx.ok(data);
        });
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new RobotClient().start(app);
    }
}
