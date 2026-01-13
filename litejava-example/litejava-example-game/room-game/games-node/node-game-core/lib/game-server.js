const http = require('http');
const crypto = require('crypto');
const url = require('url');
const { WebSocketServer } = require('ws');
const { Cmd, ErrCode } = require('./constants');
const RoomManager = require('./room-manager');
const SessionManager = require('./session-manager');
const SettlementService = require('./settlement-service');

/**
 * 游戏服务器基类
 * 
 * 子类需要实现:
 * - getGameName() - 游戏名称
 * - onGameCmd(userId, room, cmd, data) - 处理游戏命令
 * - startGame(room) - 开始游戏
 */
class GameServer {
    constructor(config) {
        this.config = config;
        this.roomManager = new RoomManager(config.game.maxPlayers);
        this.sessionManager = new SessionManager();
        
        const accountUrl = config.account?.url || 'http://localhost:8101';
        this.settlement = new SettlementService(accountUrl);
    }

    // ==================== 子类必须实现 ====================

    getGameName() {
        throw new Error('subclass must implement getGameName()');
    }

    onGameCmd(userId, room, cmd, data) {
        // 子类实现
    }

    startGame(room) {
        throw new Error('subclass must implement startGame()');
    }

    // ==================== 工具方法 ====================

    md5Sign(...parts) {
        return crypto.createHash('md5')
            .update(parts.join('') + this.config.hall.priKey)
            .digest('hex');
    }

    send(ws, cmd, code, data) {
        const msg = { cmd, code };
        if (data) msg.data = data;
        if (ws.readyState === 1) {
            ws.send(JSON.stringify(msg));
        }
    }

    sendTo(userId, cmd, data) {
        const ws = this.sessionManager.getWs(userId);
        if (ws && ws.readyState === 1) {
            this.send(ws, cmd, 0, data);
        }
    }

    broadcast(room, cmd, data) {
        const msg = JSON.stringify({ cmd, code: 0, data });
        room.seats.forEach(seat => {
            if (seat.userId) {
                const ws = this.sessionManager.getWs(seat.userId);
                if (ws && ws.readyState === 1) {
                    ws.send(msg);
                }
            }
        });
    }

    broadcastExclude(room, cmd, data, excludeUserId) {
        const msg = JSON.stringify({ cmd, code: 0, data });
        room.seats.forEach(seat => {
            if (seat.userId && seat.userId !== excludeUserId) {
                const ws = this.sessionManager.getWs(seat.userId);
                if (ws && ws.readyState === 1) {
                    ws.send(msg);
                }
            }
        });
    }

    // ==================== HTTP 处理 ====================

    handleHttp(req, res) {
        const parsedUrl = url.parse(req.url, true);
        const path = parsedUrl.pathname;
        const query = parsedUrl.query;

        res.setHeader('Content-Type', 'application/json');

        switch (path) {
            case '/create_room':
                this.handleCreateRoom(query, res);
                break;
            case '/enter_room':
                this.handleEnterRoom(query, res);
                break;
            case '/is_room_runing':
                this.handleIsRoomRunning(query, res);
                break;
            case '/health':
                this.handleHealth(res);
                break;
            default:
                res.end(JSON.stringify({ errcode: 404, errmsg: 'not found' }));
        }
    }

    handleCreateRoom(query, res) {
        const { userid, roomid, conf, sign } = query;
        const expected1 = this.md5Sign(userid, roomid || '', conf || '{}');
        const expected2 = this.md5Sign(userid, conf || '{}');

        if (sign !== expected1 && sign !== expected2) {
            res.end(JSON.stringify({ errcode: -1, errmsg: 'invalid sign' }));
            return;
        }

        const id = roomid || String(Date.now() % 1000000);
        this.roomManager.createRoom(id, Number(userid));

        console.log(`创建房间: ${id} by ${userid}`);
        res.end(JSON.stringify({ errcode: 0, roomid: id }));
    }

    handleEnterRoom(query, res) {
        const { userid, name, roomid, sign } = query;

        if (sign !== this.md5Sign(userid, name, roomid)) {
            res.end(JSON.stringify({ errcode: -1, errmsg: 'invalid sign' }));
            return;
        }

        const result = this.roomManager.enterRoom(roomid, Number(userid), name);

        if (result.error === 'ROOM_NOT_FOUND') {
            res.end(JSON.stringify({ errcode: ErrCode.ROOM_NOT_FOUND, errmsg: 'room not found' }));
            return;
        }
        if (result.error === 'ROOM_FULL') {
            res.end(JSON.stringify({ errcode: ErrCode.ROOM_FULL, errmsg: 'room full' }));
            return;
        }

        const token = this.sessionManager.createToken(Number(userid), roomid);

        console.log(`玩家进入房间: ${userid} -> ${roomid}`);
        res.end(JSON.stringify({ errcode: 0, token }));
    }

    handleIsRoomRunning(query, res) {
        res.end(JSON.stringify({
            errcode: 0,
            runing: this.roomManager.hasRoom(query.roomid)
        }));
    }

    handleHealth(res) {
        const { server } = this.config;
        res.end(JSON.stringify({
            errcode: 0,
            status: 'UP',
            serverId: `${server.clientip}:${server.wsPort}`
        }));
    }

    // ==================== WebSocket 处理 ====================

    handleWsMessage(ws, msg) {
        const { cmd, data } = msg;

        switch (cmd) {
            case Cmd.LOGIN:
                this.onLogin(ws, data);
                break;
            case Cmd.PING:
                this.send(ws, Cmd.PING, 0, null);
                break;
            case Cmd.READY:
                this.onReady(ws);
                break;
            case Cmd.ROOM_EXIT:
                this.onExit(ws);
                break;
            case Cmd.CHAT_SEND:
                this.onChat(ws, data);
                break;
            default:
                this.handleGameCmd(ws, cmd, data);
        }
    }

    handleGameCmd(ws, cmd, data) {
        const userId = this.sessionManager.getUserId(ws);
        if (!userId) return;

        const roomId = this.roomManager.getUserRoom(userId);
        const room = this.roomManager.getRoom(roomId);
        if (!room) return;

        this.onGameCmd(userId, room, cmd, data);
    }

    onLogin(ws, data) {
        const tokenInfo = this.sessionManager.validateToken(data.token);
        if (!tokenInfo) {
            this.send(ws, Cmd.LOGIN, ErrCode.INVALID_TOKEN, null);
            return;
        }

        const { userId, roomId } = tokenInfo;
        const room = this.roomManager.getRoom(roomId);
        if (!room) {
            this.send(ws, Cmd.LOGIN, ErrCode.ROOM_NOT_FOUND, null);
            return;
        }

        this.sessionManager.bind(userId, ws);
        this.roomManager.setOnline(room, userId, true);

        const gameState = this.getGameState ? this.getGameState(room, userId) : null;
        this.send(ws, Cmd.LOGIN, 0, { roomId, seats: room.seats, game: gameState });

        this.broadcastExclude(room, Cmd.USER_STATE, { userId, online: true }, userId);
        console.log(`玩家登录: ${userId} -> 房间 ${roomId}`);
    }

    onReady(ws) {
        const userId = this.sessionManager.getUserId(ws);
        if (!userId) return;

        const roomId = this.roomManager.getUserRoom(userId);
        const room = this.roomManager.getRoom(roomId);
        if (!room || room.game) return;

        const seat = this.roomManager.setReady(room, userId, true);
        this.broadcast(room, Cmd.USER_READY, { userId, seatIndex: seat.seatIndex });

        if (this.roomManager.isAllReady(room)) {
            this.startGame(room);
        }
    }

    onExit(ws) {
        const userId = this.sessionManager.getUserId(ws);
        if (!userId) return;

        const roomId = this.roomManager.getUserRoom(userId);
        const room = this.roomManager.getRoom(roomId);
        if (!room || room.game) return;

        this.broadcastExclude(room, Cmd.USER_EXIT, { userId }, userId);
        this.roomManager.exitRoom(userId);
        this.send(ws, Cmd.ROOM_EXIT, 0, null);
    }

    onChat(ws, data) {
        const userId = this.sessionManager.getUserId(ws);
        if (!userId) return;

        const roomId = this.roomManager.getUserRoom(userId);
        const room = this.roomManager.getRoom(roomId);
        if (!room) return;

        this.broadcast(room, Cmd.CHAT_MSG, { userId, content: data.content });
    }

    onDisconnect(ws) {
        const userId = this.sessionManager.unbind(ws);
        if (!userId) return;

        const roomId = this.roomManager.getUserRoom(userId);
        if (roomId) {
            const room = this.roomManager.getRoom(roomId);
            if (room) {
                this.roomManager.setOnline(room, userId, false);
                this.broadcastExclude(room, Cmd.USER_STATE, { userId, online: false }, userId);
            }
        }
        console.log(`玩家断开: ${userId}`);
    }

    // ==================== 服务注册 ====================

    getServerId() {
        const { server } = this.config;
        return `${server.clientip}:${server.wsPort}`;
    }

    getLoad() {
        return this.roomManager.roomCount * 10 + this.roomManager.playerCount;
    }

    register() {
        const { server, game, hall } = this.config;
        const registerUrl = `${hall.url}/register_gs?clientip=${server.clientip}&clientport=${server.wsPort}&httpPort=${server.httpPort}&load=${this.getLoad()}&gameType=${game.type}`;

        http.get(registerUrl, () => console.log('注册到 HallServer 成功'))
            .on('error', (e) => console.log('注册失败:', e.message));
    }

    heartbeat() {
        const { hall } = this.config;
        http.get(`${hall.url}/heartbeat?id=${this.getServerId()}&load=${this.getLoad()}`)
            .on('error', () => this.register());
    }

    // ==================== 启动服务器 ====================

    start() {
        const { server } = this.config;

        // HTTP 服务
        const httpServer = http.createServer((req, res) => this.handleHttp(req, res));

        // WebSocket 服务
        const wss = new WebSocketServer({ server: httpServer, path: '/game' });

        wss.on('connection', (ws) => {
            ws.on('message', (data) => {
                try {
                    const msg = JSON.parse(data);
                    this.handleWsMessage(ws, msg);
                } catch (e) {
                    console.error('消息解析错误:', e);
                }
            });
            ws.on('close', () => this.onDisconnect(ws));
        });

        // 启动
        httpServer.listen(server.httpPort, () => {
            console.log(`=== ${this.getGameName()} [${this.getServerId()}] ===`);
            console.log(`WebSocket: ws://${server.clientip}:${server.wsPort}/game`);
            console.log(`HTTP: http://${server.clientip}:${server.httpPort}`);

            this.register();
            setInterval(() => this.heartbeat(), 5000);
        });
    }
}

module.exports = GameServer;
