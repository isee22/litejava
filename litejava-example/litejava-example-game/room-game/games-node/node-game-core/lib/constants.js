/**
 * 协议命令号 (与 Java Cmd.java 一致)
 */
const Cmd = {
    // 系统 (1-99)
    LOGIN: 1,
    REGISTER: 2,
    LOGOUT: 3,
    PING: 4,
    KICK: 5,

    // 房间 (100-149)
    ROOM_LIST: 100,
    ROOM_CREATE: 102,
    ROOM_JOIN: 104,
    ROOM_EXIT: 106,
    ROOM_KICK: 108,
    ROOM_INFO: 110,

    // 聊天 (150-199)
    CHAT_SEND: 150,
    CHAT_MSG: 151,

    // 匹配 (300-499)
    MATCH_START: 300,
    MATCH_CANCEL: 302,
    MATCH_SUCCESS: 305,

    // 游戏通用 (500-599)
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
};

/**
 * 错误码 (与 Java ErrCode.java 一致)
 */
const ErrCode = {
    OK: 0,
    UNKNOWN: 1,
    NOT_LOGIN: 2,
    NOT_IN_ROOM: 3,
    ALREADY_MATCHING: 4,
    NO_SERVER: 5,
    KICKED: 6,
    INVALID_TOKEN: 7,

    ROOM_NOT_FOUND: 20,
    ROOM_FULL: 21,
    ROOM_NOT_IN: 22,
    ROOM_ALREADY_IN: 23,

    GAME_NOT_STARTED: 40,
    GAME_ALREADY_STARTED: 41,
    GAME_OVER: 42,
    NOT_YOUR_TURN: 43,
    INVALID_ACTION: 44
};

module.exports = { Cmd, ErrCode };
