/**
 * WebSocket 管理器
 * 
 * BabyKylin 模式架构：
 * 
 * 1. 大厅阶段：客户端通过 HTTP 与 HallServer 通信
 *    - 登录/注册 -> AccountServer (HTTP)
 *    - 创建房间/加入房间 -> HallServer (HTTP)
 * 
 * 2. 游戏阶段：客户端直连 GameServer (WebSocket)
 *    - HallServer 返回 wsUrl + token + time + sign
 *    - 客户端用 token + roomid + time + sign 连接 GameServer
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

  // ==================== 游戏通用 (500-599) ====================
  USER_JOIN: 500,
  USER_EXIT: 501,
  USER_ONLINE: 502,
  USER_OFFLINE: 503,
  READY: 504,
  CANCEL_READY: 505,
  TRUSTEESHIP_ON: 506,
  TRUSTEESHIP_OFF: 507,
  GAME_START: 510,
  GAME_OVER: 511,
  GAME_STATE: 512,
  DEAL: 520,
  DRAW: 521,
  TURN: 522,
  RECONNECT: 530,

  // ==================== 斗地主 (1000-1099) ====================
  DDZ_BID: 1001,        // 叫地主
  DDZ_BID_RESULT: 1002, // 叫地主结果
  DDZ_PLAY: 1003,       // 出牌
  DDZ_PLAY_RESULT: 1004,// 出牌结果
  DDZ_PASS: 1005,       // 不出

  // ==================== 五子棋 (1100-1199) ====================
  GOBANG_MOVE: 1100,    // 落子
  GOBANG_RESULT: 1101   // 落子结果
}

// 错误码定义 (与服务端 ErrCode.java 对应)
export const ErrCode = {
  SUCCESS: 0,
  UNKNOWN: 1,
  NOT_LOGIN: 10,
  INVALID_ACTION: 40,
  NOT_YOUR_TURN: 41,
  GAME_NOT_STARTED: 42,
  GAME_ALREADY_STARTED: 43,
  ROOM_FULL: 44,
  INVALID_CARDS: 45,
  CANNOT_PASS: 46,
  CANNOT_BEAT: 47,
  ROOM_NOT_FOUND: 50,
  ROOM_ALREADY_IN: 51,
  NOT_IN_ROOM: 52,
  NO_SERVER: 60
}

// 错误码对应的中文提示
export const ErrMsg = {
  [ErrCode.UNKNOWN]: '未知错误',
  [ErrCode.NOT_LOGIN]: '未登录',
  [ErrCode.INVALID_ACTION]: '无效操作',
  [ErrCode.NOT_YOUR_TURN]: '还没轮到你',
  [ErrCode.GAME_NOT_STARTED]: '游戏未开始',
  [ErrCode.GAME_ALREADY_STARTED]: '游戏已开始',
  [ErrCode.ROOM_FULL]: '房间已满',
  [ErrCode.INVALID_CARDS]: '无效的牌型',
  [ErrCode.CANNOT_PASS]: '必须出牌',
  [ErrCode.CANNOT_BEAT]: '打不过上家的牌',
  [ErrCode.ROOM_NOT_FOUND]: '房间不存在',
  [ErrCode.ROOM_ALREADY_IN]: '已在房间中',
  [ErrCode.NOT_IN_ROOM]: '不在房间中',
  [ErrCode.NO_SERVER]: '没有可用服务器'
}

// 获取错误提示
export function getErrMsg(code) {
  return ErrMsg[code] || `错误码: ${code}`
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
 * 开发环境: Vite 代理到各服务端口 (无需 Nginx)
 *   /api/account/* -> localhost:8101
 *   /api/hall/*    -> localhost:8201
 *   WebSocket      -> 直连 GameServer (HallServer 返回的地址)
 * 
 * 生产环境: Nginx 统一入口 (可选)
 *   /api/account/* -> AccountServer
 *   /api/hall/*    -> HallServer
 *   /ws/game/*     -> GameServer (动态路由)
 */
const API_CONFIG = {
  // Account Server (通过 Vite/Nginx 代理)
  accountServer: `${location.origin}/api/account`,
  
  // Hall Server (通过 Vite/Nginx 代理)
  hallServer: `${location.origin}/api/hall`
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
   * 快速开始 (有房间就加入，没房间就创建)
   * @returns {Promise<{roomId, token, wsUrl}>}
   */
  async quickStart(userId, gameType, roomLevel) {
    return httpPost(`${API_CONFIG.hallServer}/quick_start`, { userId, gameType, roomLevel })
  },
  
  /**
   * 创建房间
   * @returns {Promise<{roomId, token, wsUrl}>}
   */
  async createRoom(userId, gameType) {
    return httpPost(`${API_CONFIG.hallServer}/create_room`, { userId, gameType })
  },
  
  /**
   * 加入房间
   * @returns {Promise<{roomId, token, time, sign, wsUrl}>}
   */
  async enterRoom(userId, roomId) {
    return httpPost(`${API_CONFIG.hallServer}/enter_room`, { userId, roomId })
  },
  
  /**
   * 获取用户当前房间状态 (前端使用，无签名)
   */
  async getUserRoom(userId) {
    return httpPost(`${API_CONFIG.hallServer}/get_user_room`, { userId })
  },
  
  /**
   * 清除用户房间状态 (前端使用，无签名)
   */
  async clearUserRoom(userId) {
    return httpPost(`${API_CONFIG.hallServer}/clear_user_room`, { userId })
  },
  
  // ==================== 房卡房 (私人房) ====================
  
  /**
   * 创建私人房 (房卡房)
   * 需要签名验证，用于消耗房卡创建房间
   * @param {number} userId 用户ID
   * @param {string} account 账号
   * @param {string} sign 签名
   * @param {string} gameType 游戏类型
   * @param {string} conf 房间配置 JSON
   * @param {string} name 玩家昵称
   * @returns {Promise<{roomid, ip, port, token, sign}>}
   */
  async createPrivateRoom(userId, account, sign, gameType, conf, name) {
    const params = new URLSearchParams({
      userId: userId.toString(),
      account,
      sign,
      gameType,
      conf: typeof conf === 'string' ? conf : JSON.stringify(conf),
      name: name || '玩家' + userId
    })
    return httpGet(`${API_CONFIG.hallServer}/create_private_room?${params}`)
  },
  
  /**
   * 加入私人房 (房卡房)
   * 需要签名验证
   * @param {number} userId 用户ID
   * @param {string} account 账号
   * @param {string} sign 签名
   * @param {string} roomId 房间号 (6位数字)
   * @param {string} name 玩家昵称
   * @returns {Promise<{roomid, ip, port, token, sign}>}
   */
  async enterPrivateRoom(userId, account, sign, roomId, name) {
    const params = new URLSearchParams({
      userId: userId.toString(),
      account,
      sign,
      roomid: roomId,
      name: name || '玩家' + userId
    })
    return httpGet(`${API_CONFIG.hallServer}/enter_private_room?${params}`)
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
    this.loginParams = null  // 缓存登录参数 (token, roomid, time, sign)
    this.wsUrl = null
    this.lastLoginData = null  // 缓存最后一次 LOGIN 响应
    this.pendingMessages = []  // 缓存待处理的消息 (用于页面跳转时)
  }

  /**
   * 连接 GameServer
   * @param {string} wsUrl WebSocket 地址 (从 HallServer 获取)
   * @param {object} loginParams 登录参数 {token, roomid, time, sign}
   */
  connect(wsUrl, loginParams) {
    return new Promise((resolve, reject) => {
      this.wsUrl = wsUrl
      this.loginParams = loginParams
      
      console.log('[WS] 连接 GameServer:', wsUrl)
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('[WS] 连接成功，发送登录')
        this.reconnectAttempts = 0
        // 连接后立即发送登录 (带签名)
        this.send(Cmd.LOGIN, this.loginParams)
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
    
    // 缓存 LOGIN 响应
    if (cmd === Cmd.LOGIN && code === 0) {
      this.lastLoginData = { code, ...(data || {}) }
      console.log('[WS] 缓存 LOGIN 数据:', this.lastLoginData)
    }
    
    // 缓存游戏相关消息 (用于页面跳转时)
    if (cmd === Cmd.GAME_START || cmd === Cmd.DEAL || cmd >= 1000) {
      this.pendingMessages.push({ cmd, code, ...data })
    }
    
    this.emit(cmd, { code, ...data })
  }

  /**
   * 获取并清空待处理消息
   */
  flushPendingMessages() {
    const messages = this.pendingMessages
    this.pendingMessages = []
    return messages
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
    this.loginParams = null
    this.wsUrl = null
  }

  /**
   * 重连
   */
  async reconnect() {
    if (!this.wsUrl || !this.loginParams) {
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
      await this.connect(this.wsUrl, this.loginParams)
    } catch (e) {
      console.error('[WS] 重连失败:', e)
      // 2秒后重试
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
