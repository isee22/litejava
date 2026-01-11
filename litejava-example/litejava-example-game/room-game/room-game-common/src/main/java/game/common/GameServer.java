package game.common;

import game.common.vo.*;
import litejava.App;
import litejava.plugins.websocket.WebSocketPlugin;
import litejava.plugins.websocket.WebSocketPlugin.WsSession;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * 游戏服务器基类 (BabyKylin Mode)
 * 
 * 参考 BabyKylin-SCMJ/server/game_server 实现
 * 
 * 提供通用的游戏服务器功能：
 * - WebSocket 连接管理
 * - 房间管理 (RoomMgr)
 * - 服务注册/发现 (向 HallServer 注册)
 * - 消息路由
 * 
 * 子类只需实现游戏特有逻辑：
 * - getGameName() - 游戏名称
 * - onGameCmd() - 处理游戏命令
 * - startGame() - 开始游戏
 * 
 * 通信格式: {cmd, code, data}
 * 
 * @param <G> 游戏状态类型，如 DoudizhuGame
 */
public abstract class GameServer<G> {
    
    protected App app;
    protected RoomMgr<G> roomMgr;
    
    // 服务器配置
    protected String serverId;
    protected String clientip;   // 客户端可访问的 IP (BabyKylin: clientip)
    protected int clientport;    // 客户端可访问的 WebSocket 端口 (BabyKylin: clientport)
    protected int httpPort;      // HTTP 端口
    protected String gameType;
    protected int maxPlayers;
    protected String hallServerUrl;
    protected String roomPriKey = "ROOM_PRI_KEY_2024";
    
    /** 断线托管超时时间 (毫秒)，默认 5 秒 */
    protected int trusteeshipTimeout = 5000;
    
    // 会话管理: userId <-> WebSocket Session
    private final Map<Long, WsSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    
    // ==================== 子类必须实现 ====================
    
    /**
     * 获取游戏名称，用于日志和服务注册
     * @return 游戏名称，如 "斗地主"
     */
    protected abstract String getGameName();
    
    /**
     * 处理游戏特有命令
     * 
     * 基类已处理: LOGIN, READY, CHAT_SEND, ROOM_EXIT, PING
     * 子类处理游戏特有命令，如斗地主的 BID, PLAY, PASS
     * 
     * @param userId 玩家ID
     * @param room 房间对象
     * @param cmd 命令号
     * @param data 命令数据
     */
    protected abstract void onGameCmd(long userId, RoomMgr.Room<G> room, int cmd, Map<String, Object> data);
    
    /**
     * 开始游戏
     * 
     * 当所有玩家准备后自动调用
     * 子类需要:
     * 1. 创建游戏状态对象 (如 DoudizhuGame)
     * 2. 初始化游戏 (如发牌)
     * 3. 设置 room.game = 游戏对象
     * 4. 广播游戏开始消息
     * 
     * @param room 房间对象
     */
    protected abstract void startGame(RoomMgr.Room<G> room);
    
    /**
     * 获取游戏状态（用于断线重连）
     * 
     * @param room 房间对象
     * @param seatIndex 玩家座位号
     * @return 游戏状态对象，会包含在登录响应中
     */
    protected Object getGameState(RoomMgr.Room<G> room, int seatIndex) {
        return null;
    }
    
    // ==================== 启动服务器 ====================
    
    /**
     * 启动游戏服务器 (BabyKylin 模式)
     * 
     * 流程:
     * 1. 读取配置
     * 2. 初始化房间管理器
     * 3. 启动 WebSocket 服务
     * 4. 注册 HTTP 接口
     * 5. 向 HallServer 注册
     * 6. 启动 HTTP 服务
     */
    public void start(App app) {
        this.app = app;
        
        // 读取配置
        gameType = app.conf.getString("game", "type", getGameName().toLowerCase());
        maxPlayers = app.conf.getInt("game", "maxPlayers", 4);
        trusteeshipTimeout = app.conf.getInt("game", "trusteeshipTimeout", 5000);
        hallServerUrl = app.conf.getString("hall", "url", "http://localhost:8201");
        roomPriKey = app.conf.getString("server", "priKey", roomPriKey);
        
        // 端口配置 (BabyKylin: clientport = WebSocket, httpPort = HTTP)
        int listenWsPort = Integer.getInteger("server.wsPort", app.conf.getInt("server", "wsPort", 9100));
        int listenHttpPort = Integer.getInteger("server.httpPort", app.conf.getInt("server", "httpPort", 9101));
        
        // 外部可访问地址 (BabyKylin: clientip/clientport)
        clientip = getAdvertiseValue("ADVERTISE_HOST", "server", "clientip", getLocalHost());
        clientport = getAdvertiseIntValue("ADVERTISE_WS_PORT", "server", "clientport", listenWsPort);
        httpPort = getAdvertiseIntValue("ADVERTISE_HTTP_PORT", "server", "httpPort", listenHttpPort);
        
        // 服务器 ID = clientip:clientport (BabyKylin 格式)
        serverId = clientip + ":" + clientport;
        
        // 初始化房间管理器
        roomMgr = new RoomMgr<>();
        roomMgr.maxPlayers = maxPlayers;
        
        // WebSocket 服务
        WebSocketPlugin ws = new WebSocketPlugin(listenWsPort, "/game");
        ws.onConnect = session -> app.log.info("客户端连接: " + session.id);
        ws.onMessage = this::onMessage;
        ws.onClose = this::onDisconnect;
        ws.onError = (session, err) -> app.log.error("WebSocket错误: " + err.getMessage());
        app.use(ws);
        
        // HTTP 接口 (BabyKylin 风格)
        setupHttpRoutes();
        
        // 向 HallServer 注册
        registerToHall();
        
        // 定时心跳
        startHeartbeat();
        
        app.log.info("=== " + getGameName() + " 服务器 [" + serverId + "] ===");
        app.log.info("WebSocket: ws://" + clientip + ":" + clientport + "/game");
        app.log.info("HTTP: http://" + clientip + ":" + httpPort);
        
        app.run(listenHttpPort);
    }
    
    /**
     * 向 HallServer 注册 (BabyKylin 模式)
     * 
     * 参考 room_service.js /register_gs
     * 参数: clientip, clientport, httpPort, load, gameType
     */
    private void registerToHall() {
        int load = getRoomCount() * 10 + getPlayerCount();
        String url = hallServerUrl + "/register_gs?clientip=" + clientip 
            + "&clientport=" + clientport 
            + "&httpPort=" + httpPort 
            + "&load=" + load
            + "&gameType=" + gameType;
        
        try {
            httpGet(url);
            app.log.info("注册到 HallServer 成功: " + serverId);
        } catch (Exception e) {
            app.log.error("注册到 HallServer 失败: " + e.getMessage());
        }
    }
    
    /**
     * 定时向 HallServer 发送心跳
     */
    private void startHeartbeat() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            int load = getRoomCount() * 10 + getPlayerCount();
            String url = hallServerUrl + "/heartbeat?id=" + serverId + "&load=" + load;
            try {
                httpGet(url);
            } catch (Exception e) {
                app.log.warn("心跳失败: " + e.getMessage());
                // 心跳失败时尝试重新注册
                registerToHall();
            }
            
            // 检查断线托管
            checkTrusteeship();
        }, 5, 5, TimeUnit.SECONDS);
    }
    

    /**
     * 检查断线玩家是否需要托管
     * 
     * 断线超过 trusteeshipTimeout 毫秒后自动托管
     */
    private void checkTrusteeship() {
        long now = System.currentTimeMillis();
        
        for (RoomMgr.Room<G> room : roomMgr.getAllRooms()) {
            // 只检查游戏中的房间
            if (room.game == null) continue;
            
            for (RoomMgr.Seat seat : room.seats) {
                if (seat.userId <= 0) continue;
                
                // 已托管或在线，跳过
                if (seat.trusteeship || seat.online) continue;
                
                // 检查断线时间
                if (seat.disconnectTime > 0 && now - seat.disconnectTime >= trusteeshipTimeout) {
                    seat.trusteeship = true;
                    app.log.info("玩家托管: " + seat.userId + " (断线 " + (now - seat.disconnectTime) / 1000 + "s)");
                    
                    // 通知房间其他人
                    onPlayerTrusteeship(room, seat);
                }
            }
        }
    }
    
    /**
     * 玩家进入托管状态
     * 
     * 子类可覆盖，实现托管逻辑（如 AI 代打）
     */
    protected void onPlayerTrusteeship(RoomMgr.Room<G> room, RoomMgr.Seat seat) {
        // 广播托管状态
        TrusteeshipVO vo = new TrusteeshipVO();
        vo.userId = seat.userId;
        vo.trusteeship = true;
        broadcast(room, Cmd.USER_STATE, vo);
    }
    
    /**
     * 检查玩家是否托管中
     */
    protected boolean isTrusteeship(long userId) {
        String roomId = roomMgr.getUserRoom(userId);
        if (roomId == null) return false;
        
        RoomMgr.Room<G> room = roomMgr.getRoom(roomId);
        if (room == null) return false;
        
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId == userId) {
                return seat.trusteeship;
            }
        }
        return false;
    }
    
    /**
     * 获取本机IP地址
     */
    private String getLocalHost() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    // ==================== HTTP 接口 ====================
    
    /**
     * 设置 HTTP 管理接口 (BabyKylin 风格)
     * 
     * 参考 game_server/room_service.js
     * - /create_room - 创建房间
     * - /enter_room - 进入房间
     * - /is_room_runing - 检查房间是否存在
     * - /get_server_info - 获取服务器信息
     */
    private void setupHttpRoutes() {
        // 创建房间 (由 HallServer 调用)
        // 参数: userid, roomid(可选), conf, sign
        // 返回: {errcode, roomid}
        app.get("/create_room", ctx -> {
            long userId = Long.parseLong(ctx.queryParam("userid"));
            String roomId = ctx.queryParam("roomid");  // 预生成的房间号
            String conf = ctx.queryParam("conf", "{}");
            String sign = ctx.queryParam("sign");
            
            // 验证签名 (支持两种格式)
            String expectedSign1 = md5(userId + roomId + conf + roomPriKey);  // 新格式
            String expectedSign2 = md5(userId + conf + roomPriKey);           // 旧格式
            if (!expectedSign1.equals(sign) && !expectedSign2.equals(sign)) {
                ctx.json(Map.of("errcode", -1, "errmsg", "invalid sign"));
                return;
            }
            
            // 创建房间 (支持预生成房间号)
            RoomMgr.Room<G> room = roomMgr.createRoom(userId, conf, roomId);
            if (room == null) {
                ctx.json(Map.of("errcode", 1, "errmsg", "create failed"));
                return;
            }
            
            app.log.info("创建房间: " + room.id + " by " + userId);
            ctx.json(Map.of("errcode", 0, "roomid", room.id));
        });
        
        // 进入房间 (由 HallServer 调用)
        // 参数: userid, name, roomid, sign
        // 返回: {errcode, token}
        app.get("/enter_room", ctx -> {
            long userId = Long.parseLong(ctx.queryParam("userid"));
            String name = ctx.queryParam("name", "玩家" + userId);
            String roomId = ctx.queryParam("roomid");
            String sign = ctx.queryParam("sign");
            
            // 验证签名
            String expectedSign = md5(userId + name + roomId + roomPriKey);
            if (!expectedSign.equals(sign)) {
                ctx.json(Map.of("errcode", -1, "errmsg", "invalid sign"));
                return;
            }
            
            int result = roomMgr.enterRoom(roomId, userId, name);
            if (result != 0) {
                String msg = result == 1 ? "room full" : "room not found";
                ctx.json(Map.of("errcode", result, "errmsg", msg));
                return;
            }
            
            // 生成 token (参考 tokenmgr.js)
            String token = TokenManager.create(userId, roomId, 300000);
            
            app.log.info("玩家进入房间: " + userId + " -> " + roomId);
            ctx.json(Map.of("errcode", 0, "token", token));
        });
        
        // 检查房间是否存在 (由 HallServer 调用)
        // 参数: roomid, sign
        // 返回: {errcode, runing}
        app.get("/is_room_runing", ctx -> {
            String roomId = ctx.queryParam("roomid");
            String sign = ctx.queryParam("sign");
            
            // 验证签名
            String expectedSign = md5(roomId + roomPriKey);
            if (!expectedSign.equals(sign)) {
                ctx.json(Map.of("errcode", -1, "errmsg", "invalid sign"));
                return;
            }
            
            RoomMgr.Room<G> room = roomMgr.getRoom(roomId);
            boolean runing = room != null;
            ctx.json(Map.of("errcode", 0, "runing", runing));
        });
        
        // 获取服务器信息 (由 HallServer 调用)
        // 参数: serverid, sign
        // 返回: {errcode, userroominfo}
        app.get("/get_server_info", ctx -> {
            String serverid = ctx.queryParam("serverid");
            String sign = ctx.queryParam("sign");
            
            // 验证签名
            String expectedSign = md5(serverid + roomPriKey);
            if (!expectedSign.equals(sign)) {
                ctx.json(Map.of("errcode", -1, "errmsg", "invalid sign"));
                return;
            }
            
            // 返回用户-房间映射 [userId1, roomId1, userId2, roomId2, ...]
            List<Object> userroominfo = new ArrayList<>();
            for (RoomMgr.Room<G> room : roomMgr.getAllRooms()) {
                for (RoomMgr.Seat seat : room.seats) {
                    if (seat.userId > 0) {
                        userroominfo.add(seat.userId);
                        userroominfo.add(room.id);
                    }
                }
            }
            ctx.json(Map.of("errcode", 0, "userroominfo", userroominfo));
        });
        
        // 踢人 (房主操作)
        app.post("/kick", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            String roomId = String.valueOf(req.get("roomId"));
            long ownerId = ((Number) req.get("ownerId")).longValue();
            long targetId = ((Number) req.get("targetId")).longValue();
            
            RoomMgr.Room<G> room = roomMgr.getRoom(roomId);
            if (room == null) {
                ctx.json(Map.of("errcode", 1, "errmsg", "room not found"));
                return;
            }
            
            if (room.ownerId != ownerId) {
                ctx.json(Map.of("errcode", 2, "errmsg", "not owner"));
                return;
            }
            
            if (ownerId == targetId) {
                ctx.json(Map.of("errcode", 3, "errmsg", "cannot kick self"));
                return;
            }
            
            if (room.game != null) {
                ctx.json(Map.of("errcode", 4, "errmsg", "game in progress"));
                return;
            }
            
            roomMgr.exitRoom(targetId);
            
            // 通知被踢玩家
            sendTo(targetId, Cmd.ROOM_EXIT, KickResultVO.kicked());
            
            // 通知房间其他人
            UserStateVO vo = new UserStateVO();
            vo.userId = targetId;
            vo.online = false;
            broadcast(room, Cmd.USER_EXIT, vo);
            
            ctx.json(Map.of("errcode", 0));
        });
        
        // 房间信息
        app.get("/room/:roomId", ctx -> {
            String roomId = ctx.pathParam("roomId");
            RoomMgr.Room<G> room = roomMgr.getRoom(roomId);
            if (room == null) {
                ctx.json(Map.of("errcode", 1, "errmsg", "room not found"));
                return;
            }
            
            RoomInfoVO info = new RoomInfoVO();
            info.roomId = room.id;
            info.ownerId = room.ownerId;
            info.playerCount = roomMgr.getPlayerCount(room);
            info.maxPlayers = roomMgr.maxPlayers;
            info.gaming = room.game != null;
            info.seats = getSeatsVO(room);
            ctx.json(Map.of("errcode", 0, "data", info));
        });
        
        // 服务器状态
        app.get("/status", ctx -> {
            GameServerStatusVO status = new GameServerStatusVO();
            status.serverId = serverId;
            status.rooms = roomMgr.getTotalRooms();
            status.players = 0;
            for (RoomMgr.Room<G> room : roomMgr.getAllRooms()) {
                status.players += roomMgr.getPlayerCount(room);
            }
            ctx.json(Map.of("errcode", 0, "data", status));
        });
        
        // 健康检查
        app.get("/health", ctx -> {
            ctx.json(Map.of("errcode", 0, "status", "UP", "serverId", serverId));
        });
    }
    
    // ==================== WebSocket 消息处理 ====================
    
    /**
     * 处理 WebSocket 消息
     * 
     * 消息格式: {cmd: 命令号, data: 数据}
     */
    private void onMessage(WsSession session, String jsonStr) {
        Map<String, Object> msg = app.json.parseMap(jsonStr);
        int cmd = ((Number) msg.get("cmd")).intValue();
        Map<String, Object> data = (Map<String, Object>) msg.getOrDefault("data", new HashMap<>());
        
        try {
            handleCmd(session, cmd, data);
        } catch (GameException e) {
            send(session, cmd, e.code, null);
        } catch (Exception e) {
            app.log.error("处理消息异常: " + e.getMessage());
            send(session, cmd, ErrCode.UNKNOWN, null);
        }
    }
    
    /**
     * 命令路由
     * 
     * 基类处理通用命令:
     * - LOGIN (1) - 登录/进入房间
     * - READY (413) - 准备
     * - CHAT_SEND (300) - 发送聊天
     * - ROOM_EXIT (104) - 退出房间
     * - PING (4) - 心跳
     * 
     * 其他命令交给子类 onGameCmd() 处理
     */
    private void handleCmd(WsSession session, int cmd, Map<String, Object> data) {
        // 登录不需要已登录
        if (cmd == Cmd.LOGIN) {
            onLogin(session, data);
            return;
        }
        
        // 其他命令需要已登录
        Long userId = sessionUsers.get(session.id);
        if (userId == null) {
            GameException.error(ErrCode.NOT_LOGIN);
        }
        
        // 需要在房间中
        String roomId = roomMgr.getUserRoom(userId);
        RoomMgr.Room<G> room = roomId != null ? roomMgr.getRoom(roomId) : null;
        if (room == null) {
            GameException.error(ErrCode.NOT_IN_ROOM);
        }
        
        // 通用命令
        switch (cmd) {
            case Cmd.READY:
                onReady(userId, room);
                return;
            case Cmd.CHAT_SEND:
                onChat(userId, room, data);
                return;
            case Cmd.ROOM_EXIT:
                onExit(userId, session, room);
                return;
            case Cmd.PING:
                send(session, Cmd.PING, null);
                return;
        }
        
        // 游戏特有命令，交给子类处理
        onGameCmd(userId, room, cmd, data);
    }
    
    /**
     * 处理登录/进入房间
     * 
     * 请求: {token} 或 {userId, roomId} (兼容旧版)
     * 响应: {roomId, seats, game}
     */
    private void onLogin(WsSession session, Map<String, Object> data) {
        long userId;
        String roomId;
        
        // 优先使用 token 验证 (BabyKylin 模式)
        String token = (String) data.get("token");
        if (token != null && !token.isEmpty()) {
            TokenManager.TokenInfo tokenInfo = TokenManager.parse(token);
            if (!tokenInfo.valid) {
                send(session, Cmd.LOGIN, ErrCode.INVALID_TOKEN, null);
                return;
            }
            userId = tokenInfo.userId;
            roomId = tokenInfo.roomId;
            TokenManager.markUsed(token);
        } else {
            // 兼容旧版: 直接传 userId + roomId
            userId = ((Number) data.get("userId")).longValue();
            roomId = String.valueOf(data.get("roomId"));
        }
        
        // 验证房间存在
        RoomMgr.Room<G> room = roomMgr.getRoom(roomId);
        if (room == null) {
            send(session, Cmd.LOGIN, ErrCode.ROOM_NOT_FOUND, null);
            return;
        }
        
        // 验证玩家在房间中
        int seatIndex = -1;
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId == userId) {
                seatIndex = seat.seatIndex;
                seat.online = true;
                break;
            }
        }
        
        if (seatIndex < 0) {
            send(session, Cmd.LOGIN, ErrCode.NOT_IN_ROOM, null);
            return;
        }
        
        // 绑定会话
        bind(userId, session);
        
        // 构建响应
        RoomStateVO vo = new RoomStateVO();
        vo.roomId = room.id;
        vo.seats = getSeatsVO(room);
        vo.game = getGameState(room, seatIndex);  // 断线重连时恢复游戏状态
        
        send(session, Cmd.LOGIN, vo);
        
        // 通知其他人该玩家上线
        UserStateVO stateVO = new UserStateVO();
        stateVO.userId = userId;
        stateVO.online = true;
        broadcastExclude(room, Cmd.USER_STATE, stateVO, userId);
        
        app.log.info("玩家登录: " + userId + " -> 房间 " + roomId);
    }
    
    /**
     * 处理准备
     * 
     * 当所有玩家都准备后，自动开始游戏
     */
    private void onReady(long userId, RoomMgr.Room<G> room) {
        // 游戏中不能准备
        if (room.game != null) return;
        
        roomMgr.setReady(userId, true);
        
        // 广播准备状态
        ReadyVO vo = new ReadyVO();
        vo.userId = userId;
        vo.seatIndex = roomMgr.getUserSeat(userId);
        broadcast(room, Cmd.USER_READY, vo);
        
        // 检查是否全部准备
        if (roomMgr.isAllReady(room)) {
            startGame(room);
        }
    }
    
    /**
     * 处理聊天
     */
    private void onChat(long userId, RoomMgr.Room<G> room, Map<String, Object> data) {
        String content = (String) data.get("content");
        if (content == null || content.isEmpty()) return;
        
        ChatVO vo = new ChatVO();
        vo.userId = userId;
        vo.content = content;
        broadcast(room, Cmd.CHAT_MSG, vo);
    }
    
    /**
     * 处理退出房间
     */
    private void onExit(long userId, WsSession session, RoomMgr.Room<G> room) {
        // 游戏中不能退出
        if (room.game != null) return;
        
        // 通知其他人
        UserStateVO vo = new UserStateVO();
        vo.userId = userId;
        vo.online = false;
        broadcastExclude(room, Cmd.USER_EXIT, vo, userId);
        
        // 退出房间
        roomMgr.exitRoom(userId);
        unbind(userId);
        send(session, Cmd.ROOM_EXIT, null);
        session.close();
    }
    
    /**
     * 处理断开连接
     * 
     * 标记玩家离线，记录断线时间（支持断线重连和托管）
     */
    private void onDisconnect(WsSession session) {
        Long userId = sessionUsers.get(session.id);
        if (userId == null) return;
        
        String roomId = roomMgr.getUserRoom(userId);
        if (roomId != null) {
            RoomMgr.Room<G> room = roomMgr.getRoom(roomId);
            if (room != null) {
                // 标记离线，记录断线时间
                for (RoomMgr.Seat seat : room.seats) {
                    if (seat.userId == userId) {
                        seat.online = false;
                        seat.disconnectTime = System.currentTimeMillis();
                        break;
                    }
                }
                // 通知其他人
                UserStateVO vo = new UserStateVO();
                vo.userId = userId;
                vo.online = false;
                broadcastExclude(room, Cmd.USER_STATE, vo, userId);
            }
        }
        
        unbind(userId);
        app.log.info("玩家断开: " + userId);
    }
    
    // ==================== 会话管理 ====================
    
    /**
     * 绑定用户和会话
     * 
     * 如果用户已有旧会话，会踢掉旧连接
     */
    private void bind(long userId, WsSession session) {
        WsSession old = userSessions.get(userId);
        if (old != null && old != session) {
            sessionUsers.remove(old.id);
            if (old.isOpen) old.close();
        }
        userSessions.put(userId, session);
        sessionUsers.put(session.id, userId);
    }
    
    /**
     * 解绑用户和会话
     */
    private void unbind(long userId) {
        WsSession session = userSessions.remove(userId);
        if (session != null) sessionUsers.remove(session.id);
    }
    
    // ==================== 消息工具 ====================
    
    /**
     * 获取座位信息列表
     */
    private List<SeatVO> getSeatsVO(RoomMgr.Room<G> room) {
        List<SeatVO> list = new ArrayList<>();
        for (RoomMgr.Seat s : room.seats) {
            SeatVO vo = new SeatVO();
            vo.seatIndex = s.seatIndex;
            vo.userId = s.userId;
            vo.name = s.name;
            vo.ready = s.ready;
            vo.online = s.online;
            list.add(vo);
        }
        return list;
    }
    
    /**
     * 发送成功响应
     */
    private void send(WsSession session, int cmd, Object data) {
        send(session, cmd, 0, data);
    }
    
    /**
     * 发送响应（带错误码）
     */
    private void send(WsSession session, int cmd, int code, Object data) {
        VO vo = code == 0 ? VO.ok(cmd, data) : VO.fail(cmd, code);
        String json = app.json.stringify(vo);
        session.send(json);
    }
    
    /**
     * 发送消息给指定用户
     */
    protected void sendTo(long userId, int cmd, Object data) {
        WsSession session = userSessions.get(userId);
        if (session != null && session.isOpen) {
            send(session, cmd, data);
        }
    }
    
    /**
     * 广播消息给房间所有人
     */
    protected void broadcast(RoomMgr.Room<G> room, int cmd, Object data) {
        VO vo = VO.ok(cmd, data);
        String json = app.json.stringify(vo);
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                WsSession session = userSessions.get(seat.userId);
                if (session != null && session.isOpen) {
                    session.send(json);
                }
            }
        }
    }
    
    /**
     * 广播消息给房间所有人（排除指定用户）
     */
    protected void broadcastExclude(RoomMgr.Room<G> room, int cmd, Object data, long exclude) {
        VO vo = VO.ok(cmd, data);
        String json = app.json.stringify(vo);
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0 && seat.userId != exclude) {
                WsSession session = userSessions.get(seat.userId);
                if (session != null && session.isOpen) {
                    session.send(json);
                }
            }
        }
    }
    
    /**
     * 游戏结束后调用
     * 
     * 重置房间状态，等待下一局
     */
    protected void onGameOver(RoomMgr.Room<G> room) {
        room.game = null;
        for (RoomMgr.Seat seat : room.seats) {
            seat.ready = false;
        }
    }
    
    // ==================== 钩子方法 (子类可覆盖) ====================
    
    /**
     * 游戏开始时调用
     * 
     * 子类可覆盖，用于:
     * - 保存断线重连快照 (RoomSnapshot)
     * - 创建游戏录像 (GameReplay)
     */
    protected void onGameStarted(RoomMgr.Room<G> room) {
    }
    
    /**
     * 游戏结束时调用
     * 
     * 子类可覆盖，用于:
     * - 记录战绩 (PlayerData)
     * - 保存录像 (GameReplay)
     * - 清理快照 (RoomSnapshot)
     * 
     * @param room 房间
     * @param winnerSeat 获胜者座位
     * @param scores 各玩家得分
     */
    protected void onGameEnded(RoomMgr.Room<G> room, int winnerSeat, int[] scores) {
    }
    
    /**
     * 获取游戏类型标识
     */
    protected String getGameType() {
        return gameType;
    }
    
    /**
     * 获取最大玩家数
     */
    protected int getMaxPlayers() {
        return maxPlayers;
    }
    
    /**
     * 获取服务器ID
     */
    public String getServerId() {
        return serverId;
    }
    
    /**
     * 获取房间数量
     */
    public int getRoomCount() {
        return roomMgr.getTotalRooms();
    }
    
    /**
     * 获取玩家数量
     */
    public int getPlayerCount() {
        int count = 0;
        for (RoomMgr.Room<G> room : roomMgr.getAllRooms()) {
            count += roomMgr.getPlayerCount(room);
        }
        return count;
    }
    
    /**
     * HTTP GET 请求
     */
    private String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    /**
     * MD5 签名
     */
    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
    
    /**
     * 获取广播地址配置 (字符串)
     */
    private String getAdvertiseValue(String envKey, String section, String key, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return app.conf.getString(section, key, defaultValue);
    }
    
    /**
     * 获取广播地址配置 (整数)
     */
    private int getAdvertiseIntValue(String envKey, String section, String key, int defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isEmpty()) {
            try {
                return Integer.parseInt(env);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return app.conf.getInt(section, key, defaultValue);
    }
}
