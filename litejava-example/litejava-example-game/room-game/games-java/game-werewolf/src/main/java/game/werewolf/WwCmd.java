package game.werewolf;

/**
 * 狼人杀命令号定义
 * 
 * 命令号范围: 700-799
 */
public class WwCmd {
    
    // ==================== 夜晚阶段 ====================
    
    /** 狼人杀人 - 请求: {targetId} */
    public static final int WOLF_KILL = 701;
    
    /** 狼人杀人结果 - 响应: {targetId, confirmed} */
    public static final int WOLF_KILL_RESULT = 702;
    
    /** 预言家查验 - 请求: {targetId} */
    public static final int SEER_CHECK = 703;
    
    /** 预言家查验结果 - 响应: {targetId, isWolf} */
    public static final int SEER_CHECK_RESULT = 704;
    
    /** 女巫使用药水 - 请求: {useAntidote, usePoison, poisonTarget} */
    public static final int WITCH_USE = 705;
    
    /** 女巫使用结果 - 响应: {antidoteUsed, poisonUsed} */
    public static final int WITCH_USE_RESULT = 706;
    
    /** 守卫守护 - 请求: {targetId} */
    public static final int GUARD_PROTECT = 707;
    
    /** 守卫守护结果 - 响应: {targetId} */
    public static final int GUARD_PROTECT_RESULT = 708;
    
    /** 猎人技能 - 请求: {targetId} */
    public static final int HUNTER_SHOOT = 709;
    
    /** 猎人技能结果 - 响应: {targetId} */
    public static final int HUNTER_SHOOT_RESULT = 710;
    
    // ==================== 白天阶段 ====================
    
    /** 发言 - 请求: {content} */
    public static final int SPEAK = 720;
    
    /** 发言广播 - 响应: {userId, seatIndex, content} */
    public static final int SPEAK_BROADCAST = 721;
    
    /** 投票 - 请求: {targetSeat} */
    public static final int VOTE = 722;
    
    /** 投票结果 - 响应: {votes, eliminatedSeat} */
    public static final int VOTE_RESULT = 723;
    
    /** 遗言 - 请求: {content} */
    public static final int LAST_WORDS = 724;
    
    /** 遗言广播 - 响应: {userId, seatIndex, content} */
    public static final int LAST_WORDS_BROADCAST = 725;
    
    // ==================== 阶段通知 ====================
    
    /** 阶段变更 - 响应: {phase, day, timeout} */
    public static final int PHASE_CHANGE = 730;
    
    /** 死亡通知 - 响应: {deadSeats, reason} */
    public static final int DEATH_ANNOUNCE = 731;
    
    /** 游戏结束 - 响应: {winner, roles} */
    public static final int GAME_OVER = 732;
    
    /** 角色分配 - 响应: {role, roleDesc} */
    public static final int ROLE_ASSIGN = 733;
    
    /** 轮到行动 - 响应: {action, timeout, targets} */
    public static final int YOUR_TURN = 734;
    
    /** 狼人同伴 - 响应: {wolfSeats} */
    public static final int WOLF_TEAMMATES = 735;
    
    // ==================== 语音相关 ====================
    
    /** 开始语音 - 请求: {} */
    public static final int VOICE_START = 750;
    
    /** 结束语音 - 请求: {} */
    public static final int VOICE_END = 751;
    
    /** 语音信令 - 请求/响应: {type, sdp, candidate} */
    public static final int VOICE_SIGNAL = 752;
}
