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
        public int level = 1;              // 场次级别 (1=初级, 2=中级, 3=高级)
        public int robotCount;             // 机器人数量
        public int thinkTimeMin = 800;     // 最小思考时间(ms)
        public int thinkTimeMax = 2500;    // 最大思考时间(ms)
        
        public String getPoolKey() {
            return gameType + ":" + level;
        }
    }
    
    public void start(App app) {
        this.app = app;
        this.manager = new RobotManager(app);
        
        int httpPort = app.conf.getInt("server", "httpPort", 8900);
        int defaultCount = app.conf.getInt("robot", "count", 100);
        String defaultGame = app.conf.getString("robot", "gameType", "doudizhu");
        int defaultLevel = app.conf.getInt("robot", "level", 1);
        
        // 注册 AI 策略
        GameAI.register("doudizhu", new DoudizhuAI());
        GameAI.register("gobang", new GobangAI());
        GameAI.register("mahjong", new MahjongAI());
        GameAI.register("texas", new TexasAI());
        GameAI.register("niuniu", new NiuniuAI());
        
        // 根路径重定向到 dashboard
        app.get("/", ctx -> ctx.redirect("/index.html"));
        
        // 静态文件服务 (dashboard)
        app.use(new StaticFilePlugin("/", "static"));
        
        // HTTP 管理接口
        setupHttpRoutes();
        
        // 定时检查机器人状态
        scheduler.scheduleAtFixedRate(this::checkRobots, 10, 10, TimeUnit.SECONDS);
        
        app.log.info("=== 机器人客户端 ===");
        app.log.info("管理接口: http://localhost:" + httpPort);
        
        // 启动后自动创建机器人
        scheduler.schedule(() -> {
            app.log.info("自动启动 " + defaultCount + " 个 " + defaultGame + ":" + defaultLevel + " 机器人");
            setGameRobots(defaultGame, defaultLevel, defaultCount, 800, 2500);
        }, 3, TimeUnit.SECONDS);
        
        app.run(httpPort);
    }
    
    /**
     * 设置游戏机器人数量
     */
    private void setGameRobots(String gameType, int level, int count, int thinkMin, int thinkMax) {
        String poolKey = gameType + ":" + level;
        
        GameConfig config = gameConfigs.computeIfAbsent(poolKey, k -> {
            GameConfig c = new GameConfig();
            c.gameType = gameType;
            c.level = level;
            return c;
        });
        config.robotCount = count;
        config.thinkTimeMin = thinkMin;
        config.thinkTimeMax = thinkMax;
        
        List<Robot> pool = robotPools.computeIfAbsent(poolKey, k -> new CopyOnWriteArrayList<>());
        
        int current = pool.size();
        if (count > current) {
            // 增加机器人
            for (int i = 0; i < count - current; i++) {
                Robot robot = createRobot(gameType, level, config);
                pool.add(robot);
                robot.start();
            }
            app.log.info("[" + poolKey + "] 增加机器人: " + current + " -> " + count);
        } else if (count < current) {
            // 减少机器人
            for (int i = current - 1; i >= count; i--) {
                Robot robot = pool.remove(i);
                robot.shutdown();
            }
            app.log.info("[" + poolKey + "] 减少机器人: " + current + " -> " + count);
        }
    }
    
    /**
     * 创建机器人
     */
    private Robot createRobot(String gameType, int level, GameConfig config) {
        long robotId = robotIdSeq++;
        String name = "机器人" + (robotId - ROBOT_ID_START + 1);
        return new Robot(app, manager, robotId, name, gameType, level, config);
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
            
            int level = body.get("level") != null ? ((Number) body.get("level")).intValue() : 1;
            int count = ((Number) body.getOrDefault("count", 0)).intValue();
            int thinkMin = ((Number) body.getOrDefault("thinkMin", 500)).intValue();
            int thinkMax = ((Number) body.getOrDefault("thinkMax", 2000)).intValue();
            
            setGameRobots(gameType, level, count, thinkMin, thinkMax);
            
            String poolKey = gameType + ":" + level;
            GameConfig config = gameConfigs.get(poolKey);
            Map<String, Object> result = new HashMap<>();
            result.put("gameType", gameType);
            result.put("level", level);
            result.put("robotCount", config.robotCount);
            result.put("thinkTimeMin", config.thinkTimeMin);
            result.put("thinkTimeMax", config.thinkTimeMax);
            ctx.ok(result);
        });
        
        // 删除游戏机器人
        app.delete("/game/:gameType", ctx -> {
            String gameType = ctx.pathParam("gameType");
            String levelStr = ctx.queryParam("level", "1");
            int level = Integer.parseInt(levelStr);
            String poolKey = gameType + ":" + level;
            setGameRobots(gameType, level, 0, 500, 2000);
            gameConfigs.remove(poolKey);
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
        
        // 获取所有机器人列表
        app.get("/robots", ctx -> {
            String gameTypeFilter = ctx.queryParam("gameType");
            String stateFilter = ctx.queryParam("state");
            String pageStr = ctx.queryParam("page");
            String sizeStr = ctx.queryParam("size");
            int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
            int size = sizeStr != null ? Integer.parseInt(sizeStr) : 50;
            
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map.Entry<String, List<Robot>> entry : robotPools.entrySet()) {
                String poolKey = entry.getKey();
                if (gameTypeFilter != null && !poolKey.startsWith(gameTypeFilter)) continue;
                
                for (Robot robot : entry.getValue()) {
                    if (stateFilter != null && !robot.state.name().equalsIgnoreCase(stateFilter)) continue;
                    list.add(robotToMap(robot));
                }
            }
            
            // 分页
            int total = list.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, total);
            List<Map<String, Object>> pageList = start < total ? list.subList(start, end) : Collections.emptyList();
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("list", pageList);
            ctx.ok(result);
        });
        
        // 获取单个机器人详情
        app.get("/robot/:id", ctx -> {
            long robotId = Long.parseLong(ctx.pathParam("id"));
            Robot robot = findRobot(robotId);
            if (robot == null) {
                ctx.fail(404, "robot not found");
                return;
            }
            ctx.ok(robotToMap(robot));
        });
        
        // 统计信息
        app.get("/stats", ctx -> {
            long totalCoins = 0;
            int totalWins = 0;
            int totalLoses = 0;
            int totalGames = 0;
            Map<String, Integer> stateCount = new HashMap<>();
            
            for (List<Robot> pool : robotPools.values()) {
                for (Robot robot : pool) {
                    totalCoins += robot.coins;
                    totalWins += robot.winCount;
                    totalLoses += robot.loseCount;
                    totalGames += robot.totalGames;
                    String state = robot.state.name();
                    stateCount.put(state, stateCount.getOrDefault(state, 0) + 1);
                }
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRobots", robotPools.values().stream().mapToInt(List::size).sum());
            stats.put("totalCoins", totalCoins);
            stats.put("totalWins", totalWins);
            stats.put("totalLoses", totalLoses);
            stats.put("totalGames", totalGames);
            stats.put("winRate", totalGames > 0 ? String.format("%.1f%%", totalWins * 100.0 / totalGames) : "0%");
            stats.put("stateCount", stateCount);
            ctx.ok(stats);
        });
    }
    
    private Map<String, Object> robotToMap(Robot robot) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", robot.userId);
        m.put("name", robot.name);
        m.put("gameType", robot.gameType);
        m.put("level", robot.level);
        m.put("state", robot.state.name());
        m.put("roomId", robot.roomId);
        m.put("seatIndex", robot.seatIndex);
        m.put("coins", robot.coins);
        m.put("winCount", robot.winCount);
        m.put("loseCount", robot.loseCount);
        m.put("totalGames", robot.totalGames);
        m.put("winRate", robot.totalGames > 0 ? String.format("%.1f%%", robot.winCount * 100.0 / robot.totalGames) : "0%");
        m.put("serverAddr", robot.serverAddr);
        m.put("lastActiveTime", robot.lastActiveTime);
        return m;
    }
    
    private Robot findRobot(long robotId) {
        for (List<Robot> pool : robotPools.values()) {
            for (Robot robot : pool) {
                if (robot.userId == robotId) return robot;
            }
        }
        return null;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new RobotClient().start(app);
    }
}
