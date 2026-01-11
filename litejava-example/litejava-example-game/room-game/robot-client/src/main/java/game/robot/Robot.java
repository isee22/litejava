package game.robot;

import game.common.Cmd;
import game.robot.ai.GameAI;
import litejava.App;

import java.util.*;
import java.util.concurrent.*;

/**
 * 机器人 (BabyKylin 模式)
 * 
 * 流程:
 * 1. HTTP 调用 HallServer 匹配/创建房间
 * 2. 获取 GameServer 地址和 token
 * 3. WebSocket 连接 GameServer
 * 4. 发送 LOGIN (带 token)
 * 5. 准备 → 游戏 → 结束 → 重新匹配
 */
public class Robot {
    
    public enum State {
        IDLE, MATCHING, CONNECTING, IN_ROOM, GAMING, RECONNECTING
    }
    
    private final App app;
    private final RobotManager manager;
    private final RobotClient.GameConfig config;
    
    public final long userId;
    public final String name;
    public final String gameType;
    
    public volatile State state = State.IDLE;
    public String roomId;
    public String token;
    
    private WsClient gameWs;
    private GameAI ai;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Random random = new Random();
    
    private int reconnectCount = 0;
    private static final int MAX_RECONNECT = 5;
    private volatile boolean running = false;
    
    public Robot(App app, RobotManager manager, long userId, String name, String gameType, RobotClient.GameConfig config) {
        this.app = app;
        this.manager = manager;
        this.userId = userId;
        this.name = name;
        this.gameType = gameType;
        this.config = config;
        this.ai = GameAI.get(gameType);
    }
    
    /**
     * 启动机器人
     */
    public void start() {
        if (running) return;
        running = true;
        scheduleMatch();
    }
    
    /**
     * 调度匹配
     */
    private void scheduleMatch() {
        if (!running) return;
        
        int delay = 1000 + random.nextInt(2000);
        scheduler.schedule(this::doMatch, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 执行匹配 (HTTP 调用 HallServer)
     */
    private void doMatch() {
        if (!running) return;
        
        state = State.MATCHING;
        app.log.info("[Robot " + userId + "] 开始匹配 " + gameType);
        
        // 调用 HallServer 匹配接口
        Map<String, Object> result = manager.startMatch(userId, name, gameType);
        
        if (result == null) {
            app.log.warn("[Robot " + userId + "] 匹配失败，重试");
            manager.cancelMatch(userId);
            scheduleMatch();
            return;
        }
        
        String status = (String) result.get("status");
        if ("matching".equals(status)) {
            // 还在匹配中，继续轮询 (不要重新调用 startMatch)
            scheduler.schedule(this::pollMatch, 2000, TimeUnit.MILLISECONDS);
            return;
        }
        
        if ("matched".equals(status) || result.containsKey("roomid")) {
            // 匹配成功
            onMatchSuccess(result);
        } else if ("cancelled".equals(status)) {
            // 被取消，重新匹配
            scheduleMatch();
        } else {
            app.log.warn("[Robot " + userId + "] 匹配状态: " + status);
            manager.cancelMatch(userId);
            scheduleMatch();
        }
    }
    
    /**
     * 轮询匹配结果 (调用 startMatch 会返回已有结果)
     */
    private void pollMatch() {
        if (!running || state != State.MATCHING) return;
        
        // startMatch 会先检查 matchResults，如果有结果直接返回
        Map<String, Object> result = manager.startMatch(userId, name, gameType);
        if (result == null) {
            manager.cancelMatch(userId);
            scheduleMatch();
            return;
        }
        
        String status = (String) result.get("status");
        if ("matching".equals(status)) {
            // 继续等待
            scheduler.schedule(this::pollMatch, 2000, TimeUnit.MILLISECONDS);
        } else if ("matched".equals(status) || result.containsKey("roomid")) {
            onMatchSuccess(result);
        } else {
            manager.cancelMatch(userId);
            scheduleMatch();
        }
    }
    
    /**
     * 匹配成功，连接 GameServer
     */
    private void onMatchSuccess(Map<String, Object> data) {
        roomId = (String) data.get("roomid");
        token = (String) data.get("token");
        String ip = (String) data.get("ip");
        int port = ((Number) data.get("port")).intValue();
        
        app.log.info("[Robot " + userId + "] 匹配成功, roomId=" + roomId + ", server=" + ip + ":" + port);
        
        connectGameServer(ip, port);
    }
    
    /**
     * 连接 GameServer WebSocket
     */
    private void connectGameServer(String ip, int port) {
        if (!running) return;
        
        state = State.CONNECTING;
        
        try {
            String wsUrl = manager.buildGameWsUrl(ip, port);
            gameWs = new WsClient(wsUrl, this::onMessage, app);
            gameWs.setOnClose(this::onDisconnect);
            gameWs.setOnOpen(this::onConnected);
            gameWs.connect();
            
            // 启动心跳
            scheduler.scheduleAtFixedRate(this::heartbeat, 30, 30, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            app.log.error("[Robot " + userId + "] 连接 GameServer 失败: " + e.getMessage());
            scheduleReconnect();
        }
    }
    
    /**
     * 连接成功后发送登录 (带 token)
     */
    private void onConnected() {
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        gameWs.send(Cmd.LOGIN, data);
        app.log.info("[Robot " + userId + "] 发送登录请求 (token)");
    }
    
    /**
     * 处理消息
     */
    private void onMessage(int cmd, int code, Map<String, Object> data) {
        if (!running) return;
        
        if (cmd == Cmd.LOGIN) {
            if (code == 0 && data != null) {
                state = State.IN_ROOM;
                reconnectCount = 0;
                app.log.info("[Robot " + userId + "] 进入房间成功");
                
                if (ai != null) {
                    ai.onRoomJoin(this, data);
                }
                
                // 自动准备
                scheduleReady();
            } else {
                app.log.error("[Robot " + userId + "] 登录失败: " + code);
                state = State.IDLE;
                scheduleMatch();
            }
        } else if (cmd == Cmd.GAME_START) {
            state = State.GAMING;
            if (ai != null) {
                ai.onGameStart(this, data);
            }
        } else if (cmd == Cmd.GAME_OVER) {
            state = State.IDLE;
            roomId = null;
            token = null;
            
            if (ai != null) {
                ai.onGameOver(this, data);
            }
            
            // 关闭连接，重新匹配
            if (gameWs != null) {
                gameWs.close();
                gameWs = null;
            }
            scheduleMatch();
        } else if (cmd >= 500 && ai != null) {
            // 游戏消息，传给 AI
            int thinkTime = cmd >= 1000 ? randomThinkTime() : 0;
            if (thinkTime > 0) {
                scheduler.schedule(() -> {
                    if (running && ai != null) {
                        ai.onGameCmd(this, cmd, code, data);
                    }
                }, thinkTime, TimeUnit.MILLISECONDS);
            } else {
                ai.onGameCmd(this, cmd, code, data);
            }
        }
    }
    
    /**
     * 调度准备
     */
    private void scheduleReady() {
        if (!running || state != State.IN_ROOM) return;
        
        int delay = 500 + random.nextInt(1000);
        scheduler.schedule(this::sendReady, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 发送准备
     */
    private void sendReady() {
        if (!running || state != State.IN_ROOM) return;
        if (gameWs == null || !gameWs.isConnected()) return;
        
        gameWs.send(Cmd.READY, null);
        app.log.info("[Robot " + userId + "] 发送准备");
    }
    
    /**
     * 断开连接
     */
    private void onDisconnect() {
        if (!running) return;
        if (state == State.IDLE) return;
        
        app.log.warn("[Robot " + userId + "] 断开连接");
        
        if (state == State.GAMING || state == State.IN_ROOM) {
            // 游戏中断线，尝试重连
            scheduleReconnect();
        } else {
            // 其他状态，重新匹配
            state = State.IDLE;
            scheduleMatch();
        }
    }
    
    /**
     * 调度重连
     */
    private void scheduleReconnect() {
        if (!running) return;
        if (reconnectCount >= MAX_RECONNECT) {
            app.log.error("[Robot " + userId + "] 重连次数超限，重新匹配");
            state = State.IDLE;
            roomId = null;
            token = null;
            scheduleMatch();
            return;
        }
        
        state = State.RECONNECTING;
        reconnectCount++;
        int delay = Math.min(2000 * reconnectCount, 10000);
        
        // 重新获取房间信息
        scheduler.schedule(() -> {
            if (!running) return;
            
            if (roomId != null) {
                // 尝试重新进入房间
                Map<String, Object> result = manager.enterPrivateRoom(userId, name, roomId);
                if (result != null && result.containsKey("token")) {
                    token = (String) result.get("token");
                    String ip = (String) result.get("ip");
                    int port = ((Number) result.get("port")).intValue();
                    connectGameServer(ip, port);
                    return;
                }
            }
            
            // 重连失败，重新匹配
            state = State.IDLE;
            roomId = null;
            token = null;
            scheduleMatch();
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 心跳
     */
    private void heartbeat() {
        if (gameWs != null && gameWs.isConnected()) {
            gameWs.send(Cmd.PING, null);
        }
    }
    
    /**
     * 随机思考时间
     */
    private int randomThinkTime() {
        return config.thinkTimeMin + random.nextInt(config.thinkTimeMax - config.thinkTimeMin + 1);
    }
    
    /**
     * 发送游戏命令
     */
    public void sendGameCmd(int cmd, Map<String, Object> data) {
        if (gameWs != null && gameWs.isConnected()) {
            gameWs.send(cmd, data);
        }
    }
    
    /**
     * 停止机器人
     */
    public void shutdown() {
        running = false;
        scheduler.shutdownNow();
        if (gameWs != null) {
            gameWs.close();
            gameWs = null;
        }
        state = State.IDLE;
    }
}
