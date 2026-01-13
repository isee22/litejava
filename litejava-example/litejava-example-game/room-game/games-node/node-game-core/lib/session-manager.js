/**
 * 会话管理器
 */
class SessionManager {
    constructor() {
        this.sessions = new Map();  // userId -> ws
        this.connUser = new Map();  // ws -> userId
        this.tokens = new Map();    // token -> {userId, roomId, expires}
    }

    createToken(userId, roomId, ttl = 300000) {
        const token = `${userId}_${roomId}_${Date.now()}`;
        this.tokens.set(token, {
            userId,
            roomId,
            expires: Date.now() + ttl
        });
        return token;
    }

    validateToken(token) {
        const info = this.tokens.get(token);
        if (!info || Date.now() > info.expires) {
            return null;
        }
        this.tokens.delete(token);
        return info;
    }

    bind(userId, ws) {
        const oldWs = this.sessions.get(userId);
        if (oldWs && oldWs !== ws) {
            this.connUser.delete(oldWs);
            oldWs.close();
        }
        this.sessions.set(userId, ws);
        this.connUser.set(ws, userId);
    }

    unbind(ws) {
        const userId = this.connUser.get(ws);
        if (userId) {
            this.sessions.delete(userId);
            this.connUser.delete(ws);
        }
        return userId;
    }

    getUserId(ws) {
        return this.connUser.get(ws);
    }

    getWs(userId) {
        return this.sessions.get(userId);
    }

    isOnline(userId) {
        const ws = this.sessions.get(userId);
        return ws && ws.readyState === 1;
    }
}

module.exports = SessionManager;
