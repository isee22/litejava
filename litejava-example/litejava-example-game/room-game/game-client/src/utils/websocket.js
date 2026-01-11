/**
 * WebSocket 管理器
 * 
 * BabyKylin 模式架构：
 * 
 * 1. 大厅阶段：客户端通过 HTTP 与 HallServer 通信
 *    - 登录/注册 -> AccountServer (HTTP)
 *    - 创建房间/加入房间/匹配 -> HallServer (HTTP)
 * 
 * 2. 游戏阶段：客户端直连 GameServer (WebSocket)
 *    - HallServer 返回 wsUrl + token
 *    - 客户端用 token 连接 GameServer
 * 
 * ┌─────────────┐     HTTP      ┌─────────────┐
 * │   Client    │ ────────────> │ HallServer  │
 * └─────────────┘               └─────────────┘
 *       │                              │
 *       │ WebSocket                    │ HTTP
 *       ▼                              ▼
 * ┌─────────────┐               ┌─────────────┐
 * │ GameServer  │ <──────────── │AccountServer│
 * └─────────────┘   (注册/配置)  └─────────────┘
 */

export const Cmd = {
  // ==================== 系统 (1-99) ====================
  LOGIN: 1,
  LOGOUT: 3,
  PING: 4,
  ERROR: 99,

  // ==================== 房间 (100-149) ====================
  ROOM_LIST: 100,
  ROOM_CREATE: 102,
  ROOM_JOIN: 104,
  ROOM_EXIT: 106,
  ROOM_INFO: 110,

  // ==================== 聊天 (150-199) ====================
  CHAT_SEND: 150,
  CHAT_MSG: 151,
  CHAT_JOIN_ROOM: 152,
  CHAT_LEAVE_ROOM: 154,
  CHAT_PRIVATE: 156,

  // ==================== 其他大厅 (200-299) ====================
  SIGN_IN: 200,
  REPLAY_LIST: 210,
  REPLAY_GET: 212,

  // ==================== 匹配 (300-499) ====================
  MATCH_START: 300,
  MATCH_CANCEL: 302,
  MATCH_SUCCESS: 305,

  // ==================== 游戏通用 (500-599) ====================
  USER_JOIN: 500,
  USER_EXIT: 501,
  USER_STATE: 502,
  USER_READY: 503,
  READY: 504,
  GAME_START: 510,
  GAME_OVER: 511,
  GAME_STATE: 512,
  DEAL: 520,
  DRAW: 521,
  TURN: 522,
  RECONNECT: 530
}

export const GameType = {
  DOUDIZHU: 'doudizhu',
  MAHJONG: 'mahjong',
  GOBANG: 'gobang',
  TEXAS: 'texas',
  NIUNIU: 'niuniu',
  WEREWOLF: 'werewolf'
}

/**
 * API 配置
 * 
 * 开发环境: 通过 vite 代理 (/api/account/*, /api/hall/*)
 * 生产环境: 通过 Nginx 代理
 */
const API_CONFIG = {
  // Account Server (通过代理)
  accountServer: `${location.origin}/api/account`,
  
  // Hall Server (通过代理)
  hallServer: `${location.origin}/api/hall`,
  
  // WebSocket 代理
  wsProxy: `${location.origin}/ws/game`
}

/**
 * HTTP 请求工具
 */
async function httpPost(url, data) {
  const resp = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  return resp.json()
}

async function httpGet(url) {
  const resp = await fetch(url)
  return resp.json()
}

/**
 * 大厅 API (HTTP)
 */
export const hallApi = {
  /**
   * 登录
   */
  async login(username, password) {
    return httpPost(`${API_CONFIG.accountServer}/login`, { username, password })
  },
  
  /**
   * 注册
   */
  async register(username, password, name) {
    return httpPost(`${API_CONFIG.accountServer}/register`, { username, password, name })
  },
  
  /**
   * 获取玩家信息
   */
  async getPlayer(userId) {
    return httpGet(`${API_CONFIG.accountServer}/player/${userId}`)
  },
  
  /**
   * 获取房间配置列表
   */
  async getRoomConfigs() {
    return httpGet(`${API_CONFIG.hallServer}/room/configs`)
  },
  
  /**
   * 创建房间
   * @returns {Promise<{roomId, token, wsUrl}>}
   */
  async createRoom(userId, gameType, conf) {
    return httpPost(`${API_CONFIG.hallServer}/create_room`, { userId, gameType, conf })
  },
  
  /**
   * 加入房间
   * @returns {Promise<{roomId, token, wsUrl}>}
   */
  async enterRoom(userId, roomId, name) {
    return httpPost(`${API_CONFIG.hallServer}/enter_room`, { userId, roomId, name })
  },
  
  /**
   * 开始匹配
   * @returns {Promise<{status, roomId?, token?, wsUrl?}>}
   */
  async startMatch(userId, gameType, level, name) {
    return httpPost(`${API_CONFIG.hallServer}/match/start`, { userId, gameType, level, name })
  },
  
  /**
   * 取消匹配
   */
  async cancelMatch(userId) {
    return httpPost(`${API_CONFIG.hallServer}/match/cancel`, { userId })
  },
  
  /**
   * 轮询匹配结果
   */
  async pollMatch(userId, gameType, level, name) {
    return httpPost(`${API_CONFIG.hallServer}/match/start`, { userId, gameType, level, name })
  }
}

/**
 * WebSocket 管理器 (游戏阶段)
 * 
 * 直连 GameServer
 */
class WebSocketManager {
  constructor() {
    this.ws = null
    this.heartbeatTimer = null
    this.handlers = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.token = null
    this.wsUrl = null
  }

  /**
   * 连接 GameServer
   * @param {string} wsUrl WebSocket 地址 (从 HallServer 获取)
   * @param {string} token 连接令牌 (从 HallServer 获取)
   */
  connect(wsUrl, token) {
    return new Promise((resolve, reject) => {
      this.wsUrl = wsUrl
      this.token = token
      
      console.log('[WS] 连接 GameServer:', wsUrl)
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('[WS] 连接成功，发送登录')
        this.reconnectAttempts = 0
        // 连接后立即发送登录
        this.send(Cmd.LOGIN, { token })
      }

      this.ws.onmessage = (e) => {
        const msg = JSON.parse(e.data)
        this.handleMessage(msg)
        
        // 登录成功后 resolve
        if (msg.cmd === Cmd.LOGIN && msg.code === 0) {
          this.startHeartbeat()
          resolve(msg.data)
        } else if (msg.cmd === Cmd.LOGIN && msg.code !== 0) {
          reject(new Error('登录失败: ' + msg.code))
        }
      }

      this.ws.onclose = (e) => {
        console.log('[WS] 连接关闭', e.code, e.reason)
        this.stopHeartbeat()
        this.emit('disconnected', { code: e.code, reason: e.reason })
      }

      this.ws.onerror = (e) => {
        console.error('[WS] 连接错误', e)
        reject(e)
      }
    })
  }

  /**
   * 发送消息
   */
  send(cmd, data = {}) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const msg = JSON.stringify({ cmd, data })
      this.ws.send(msg)
      console.log('[WS] 发送:', cmd, data)
    } else {
      console.warn('[WS] 连接未就绪，无法发送:', cmd)
    }
  }

  /**
   * 处理收到的消息
   */
  handleMessage(msg) {
    const { cmd, code, data } = msg
    if (code !== 0) {
      console.error('[WS] ⬇️ 错误:', JSON.stringify(msg))
    } else {
      console.log('[WS] ⬇️ 收到:', JSON.stringify(msg))
    }
    
    this.emit(cmd, { code, ...data })
  }

  /**
   * 心跳
   */
  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      this.send(Cmd.PING, {})
    }, 30000)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 事件监听
   */
  on(event, handler) {
    if (!this.handlers.has(event)) {
      this.handlers.set(event, [])
    }
    this.handlers.get(event).push(handler)
  }

  off(event, handler) {
    const handlers = this.handlers.get(event)
    if (handlers) {
      const idx = handlers.indexOf(handler)
      if (idx >= 0) handlers.splice(idx, 1)
    }
  }

  emit(event, data) {
    const handlers = this.handlers.get(event)
    if (handlers) {
      handlers.forEach((h) => h(data))
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.token = null
    this.wsUrl = null
  }

  /**
   * 重连
   */
  async reconnect() {
    if (!this.wsUrl || !this.token) {
      console.warn('[WS] 无法重连：缺少连接信息')
      return
    }
    
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WS] 重连次数超限')
      return
    }
    
    this.reconnectAttempts++
    console.log(`[WS] 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
    
    try {
      await this.connect(this.wsUrl, this.token)
    } catch (e) {
      console.error('[WS] 重连失败:', e)
      setTimeout(() => this.reconnect(), 2000)
    }
  }

  /**
   * 检查连接状态
   */
  get isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

export const wsManager = new WebSocketManager()
