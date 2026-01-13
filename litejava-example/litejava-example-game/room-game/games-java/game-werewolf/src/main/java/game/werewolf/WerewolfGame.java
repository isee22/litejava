package game.werewolf;

import java.util.*;

/**
 * 狼人杀游戏逻辑
 * 
 * 角色配置 (8人局):
 * - 狼人 x2
 * - 预言家 x1
 * - 女巫 x1
 * - 守卫 x1
 * - 猎人 x1
 * - 村民 x2
 * 
 * 游戏流程:
 * 1. 夜晚: 狼人杀人 → 预言家查验 → 女巫用药 → 守卫守护
 * 2. 白天: 公布死讯 → 遗言 → 发言 → 投票
 * 3. 重复直到一方胜利
 * 
 * 胜利条件:
 * - 好人胜: 所有狼人死亡
 * - 狼人胜: 狼人数量 >= 好人数量
 */
public class WerewolfGame {
    
    // ==================== 角色定义 ====================
    
    public static final int ROLE_VILLAGER = 0;  // 村民
    public static final int ROLE_WOLF = 1;      // 狼人
    public static final int ROLE_SEER = 2;      // 预言家
    public static final int ROLE_WITCH = 3;     // 女巫
    public static final int ROLE_GUARD = 4;     // 守卫
    public static final int ROLE_HUNTER = 5;    // 猎人
    
    // ==================== 游戏阶段 ====================
    
    public static final int PHASE_NIGHT_START = 0;   // 夜晚开始
    public static final int PHASE_WOLF_TURN = 1;     // 狼人回合
    public static final int PHASE_SEER_TURN = 2;     // 预言家回合
    public static final int PHASE_WITCH_TURN = 3;    // 女巫回合
    public static final int PHASE_GUARD_TURN = 4;    // 守卫回合
    public static final int PHASE_DAY_START = 5;     // 白天开始
    public static final int PHASE_SPEAK = 6;         // 发言阶段
    public static final int PHASE_VOTE = 7;          // 投票阶段
    public static final int PHASE_LAST_WORDS = 8;    // 遗言阶段
    public static final int PHASE_GAME_OVER = 9;     // 游戏结束
    
    // ==================== 游戏状态 ====================
    
    /** 玩家数量 */
    public int playerCount;
    
    /** 当前天数 */
    public int day = 1;
    
    /** 当前阶段 */
    public int phase = PHASE_NIGHT_START;
    
    /** 各玩家角色 [seatIndex] -> role */
    public int[] roles;
    
    /** 各玩家存活状态 */
    public boolean[] alive;
    
    /** 女巫解药是否还有 */
    public boolean hasAntidote = true;
    
    /** 女巫毒药是否还有 */
    public boolean hasPoison = true;
    
    /** 守卫上一晚守护的人 (-1表示无) */
    public int lastGuardTarget = -1;
    
    /** 当前守护目标 */
    public int currentGuardTarget = -1;
    
    /** 狼人本轮杀人目标 */
    public int wolfKillTarget = -1;
    
    /** 狼人投票 [wolfSeat] -> targetSeat */
    public Map<Integer, Integer> wolfVotes = new HashMap<>();
    
    /** 女巫本轮是否救人 */
    public boolean witchSaved = false;
    
    /** 女巫本轮毒杀目标 */
    public int witchPoisonTarget = -1;
    
    /** 本轮死亡名单 */
    public List<Integer> nightDeaths = new ArrayList<>();
    
    /** 投票记录 [voterSeat] -> targetSeat */
    public Map<Integer, Integer> votes = new HashMap<>();
    
    /** 当前发言者座位 */
    public int currentSpeaker = -1;
    
    /** 猎人是否可以开枪 (被毒死不能开枪) */
    public boolean hunterCanShoot = true;
    
    /** 游戏胜利方 (0=未结束, 1=好人胜, 2=狼人胜) */
    public int winner = 0;
    
    // ==================== 初始化 ====================
    
    public WerewolfGame(int playerCount) {
        this.playerCount = playerCount;
        this.roles = new int[playerCount];
        this.alive = new boolean[playerCount];
        Arrays.fill(alive, true);
    }
    
    /**
     * 分配角色
     * 
     * 根据人数自动配置角色
     */
    public void assignRoles() {
        List<Integer> roleList = getRoleConfig(playerCount);
        Collections.shuffle(roleList);
        
        for (int i = 0; i < playerCount; i++) {
            roles[i] = roleList.get(i);
        }
    }
    
    /**
     * 获取角色配置
     */
    private List<Integer> getRoleConfig(int count) {
        List<Integer> list = new ArrayList<>();
        
        if (count == 6) {
            // 6人局: 2狼 1预言家 1女巫 2村民
            list.add(ROLE_WOLF);
            list.add(ROLE_WOLF);
            list.add(ROLE_SEER);
            list.add(ROLE_WITCH);
            list.add(ROLE_VILLAGER);
            list.add(ROLE_VILLAGER);
        } else if (count == 8) {
            // 8人局: 2狼 1预言家 1女巫 1守卫 1猎人 2村民
            list.add(ROLE_WOLF);
            list.add(ROLE_WOLF);
            list.add(ROLE_SEER);
            list.add(ROLE_WITCH);
            list.add(ROLE_GUARD);
            list.add(ROLE_HUNTER);
            list.add(ROLE_VILLAGER);
            list.add(ROLE_VILLAGER);
        } else if (count == 9) {
            // 9人局: 3狼 1预言家 1女巫 1守卫 1猎人 2村民
            list.add(ROLE_WOLF);
            list.add(ROLE_WOLF);
            list.add(ROLE_WOLF);
            list.add(ROLE_SEER);
            list.add(ROLE_WITCH);
            list.add(ROLE_GUARD);
            list.add(ROLE_HUNTER);
            list.add(ROLE_VILLAGER);
            list.add(ROLE_VILLAGER);
        } else {
            // 默认12人局: 4狼 1预言家 1女巫 1守卫 1猎人 4村民
            list.add(ROLE_WOLF);
            list.add(ROLE_WOLF);
            list.add(ROLE_WOLF);
            list.add(ROLE_WOLF);
            list.add(ROLE_SEER);
            list.add(ROLE_WITCH);
            list.add(ROLE_GUARD);
            list.add(ROLE_HUNTER);
            for (int i = list.size(); i < count; i++) {
                list.add(ROLE_VILLAGER);
            }
        }
        
        return list;
    }
    
    // ==================== 角色查询 ====================
    
    /**
     * 获取所有狼人座位
     */
    public List<Integer> getWolfSeats() {
        List<Integer> wolves = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            if (roles[i] == ROLE_WOLF) {
                wolves.add(i);
            }
        }
        return wolves;
    }
    
    /**
     * 获取存活的狼人座位
     */
    public List<Integer> getAliveWolfSeats() {
        List<Integer> wolves = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            if (roles[i] == ROLE_WOLF && alive[i]) {
                wolves.add(i);
            }
        }
        return wolves;
    }
    
    /**
     * 获取指定角色的座位 (-1表示不存在)
     */
    public int getRoleSeat(int role) {
        for (int i = 0; i < playerCount; i++) {
            if (roles[i] == role) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 检查角色是否存活
     */
    public boolean isRoleAlive(int role) {
        int seat = getRoleSeat(role);
        return seat >= 0 && alive[seat];
    }
    
    /**
     * 获取角色名称
     */
    public static String getRoleName(int role) {
        switch (role) {
            case ROLE_VILLAGER: return "村民";
            case ROLE_WOLF: return "狼人";
            case ROLE_SEER: return "预言家";
            case ROLE_WITCH: return "女巫";
            case ROLE_GUARD: return "守卫";
            case ROLE_HUNTER: return "猎人";
            default: return "未知";
        }
    }
    
    // ==================== 夜晚行动 ====================
    
    /**
     * 狼人投票杀人
     * 
     * @param wolfSeat 狼人座位
     * @param targetSeat 目标座位 (-1表示空刀)
     * @return 是否所有狼人已投票
     */
    public boolean wolfVote(int wolfSeat, int targetSeat) {
        if (roles[wolfSeat] != ROLE_WOLF || !alive[wolfSeat]) {
            return false;
        }
        
        wolfVotes.put(wolfSeat, targetSeat);
        
        // 检查是否所有存活狼人都投票了
        List<Integer> aliveWolves = getAliveWolfSeats();
        if (wolfVotes.size() >= aliveWolves.size()) {
            // 统计票数，选择票数最多的目标
            Map<Integer, Integer> voteCount = new HashMap<>();
            for (int target : wolfVotes.values()) {
                voteCount.merge(target, 1, Integer::sum);
            }
            
            int maxVotes = 0;
            int maxTarget = -1;
            for (Map.Entry<Integer, Integer> e : voteCount.entrySet()) {
                if (e.getValue() > maxVotes) {
                    maxVotes = e.getValue();
                    maxTarget = e.getKey();
                }
            }
            
            wolfKillTarget = maxTarget;
            return true;
        }
        
        return false;
    }
    
    /**
     * 预言家查验
     * 
     * @param targetSeat 目标座位
     * @return 是否是狼人
     */
    public boolean seerCheck(int targetSeat) {
        return roles[targetSeat] == ROLE_WOLF;
    }
    
    /**
     * 女巫使用药水
     * 
     * @param useAntidote 是否使用解药
     * @param usePoison 是否使用毒药
     * @param poisonTarget 毒药目标
     */
    public void witchUse(boolean useAntidote, boolean usePoison, int poisonTarget) {
        if (useAntidote && hasAntidote && wolfKillTarget >= 0) {
            witchSaved = true;
            hasAntidote = false;
        }
        
        if (usePoison && hasPoison && poisonTarget >= 0 && alive[poisonTarget]) {
            witchPoisonTarget = poisonTarget;
            hasPoison = false;
            // 被毒死的猎人不能开枪
            if (roles[poisonTarget] == ROLE_HUNTER) {
                hunterCanShoot = false;
            }
        }
    }
    
    /**
     * 守卫守护
     * 
     * @param targetSeat 守护目标
     * @return 是否成功
     */
    public boolean guardProtect(int targetSeat) {
        // 不能连续两晚守护同一人
        if (targetSeat == lastGuardTarget) {
            return false;
        }
        
        currentGuardTarget = targetSeat;
        return true;
    }
    
    // ==================== 结算夜晚 ====================
    
    /**
     * 结算夜晚死亡
     * 
     * @return 死亡名单
     */
    public List<Integer> resolveNight() {
        nightDeaths.clear();
        
        // 狼人杀人 (如果没被守卫守护且没被女巫救)
        if (wolfKillTarget >= 0 && !witchSaved) {
            if (currentGuardTarget != wolfKillTarget) {
                nightDeaths.add(wolfKillTarget);
                alive[wolfKillTarget] = false;
            }
        }
        
        // 女巫毒杀 (守卫不能防毒)
        if (witchPoisonTarget >= 0) {
            if (!nightDeaths.contains(witchPoisonTarget)) {
                nightDeaths.add(witchPoisonTarget);
                alive[witchPoisonTarget] = false;
            }
        }
        
        // 重置夜晚状态
        lastGuardTarget = currentGuardTarget;
        currentGuardTarget = -1;
        wolfKillTarget = -1;
        wolfVotes.clear();
        witchSaved = false;
        witchPoisonTarget = -1;
        
        return nightDeaths;
    }
    
    // ==================== 白天行动 ====================
    
    /**
     * 投票
     * 
     * @param voterSeat 投票者座位
     * @param targetSeat 目标座位 (-1表示弃票)
     */
    public void vote(int voterSeat, int targetSeat) {
        if (alive[voterSeat]) {
            votes.put(voterSeat, targetSeat);
        }
    }
    
    /**
     * 结算投票
     * 
     * @return 被投出的座位 (-1表示平票)
     */
    public int resolveVote() {
        Map<Integer, Integer> voteCount = new HashMap<>();
        
        for (int target : votes.values()) {
            if (target >= 0) {
                voteCount.merge(target, 1, Integer::sum);
            }
        }
        
        if (voteCount.isEmpty()) {
            votes.clear();
            return -1;
        }
        
        // 找出最高票
        int maxVotes = 0;
        int maxTarget = -1;
        int maxCount = 0;
        
        for (Map.Entry<Integer, Integer> e : voteCount.entrySet()) {
            if (e.getValue() > maxVotes) {
                maxVotes = e.getValue();
                maxTarget = e.getKey();
                maxCount = 1;
            } else if (e.getValue() == maxVotes) {
                maxCount++;
            }
        }
        
        votes.clear();
        
        // 平票不处决
        if (maxCount > 1) {
            return -1;
        }
        
        // 处决
        if (maxTarget >= 0) {
            alive[maxTarget] = false;
        }
        
        return maxTarget;
    }
    
    /**
     * 猎人开枪
     * 
     * @param targetSeat 目标座位
     */
    public void hunterShoot(int targetSeat) {
        if (targetSeat >= 0 && alive[targetSeat]) {
            alive[targetSeat] = false;
        }
    }
    
    // ==================== 胜负判定 ====================
    
    /**
     * 检查游戏是否结束
     * 
     * @return 0=未结束, 1=好人胜, 2=狼人胜
     */
    public int checkWinner() {
        int wolfCount = 0;
        int goodCount = 0;
        
        for (int i = 0; i < playerCount; i++) {
            if (alive[i]) {
                if (roles[i] == ROLE_WOLF) {
                    wolfCount++;
                } else {
                    goodCount++;
                }
            }
        }
        
        if (wolfCount == 0) {
            winner = 1;  // 好人胜
            return 1;
        }
        
        if (wolfCount >= goodCount) {
            winner = 2;  // 狼人胜
            return 2;
        }
        
        return 0;  // 未结束
    }
    
    // ==================== 阶段流转 ====================
    
    /**
     * 进入下一阶段
     */
    public void nextPhase() {
        switch (phase) {
            case PHASE_NIGHT_START:
                phase = PHASE_WOLF_TURN;
                break;
            case PHASE_WOLF_TURN:
                phase = isRoleAlive(ROLE_SEER) ? PHASE_SEER_TURN : nextAfterSeer();
                break;
            case PHASE_SEER_TURN:
                phase = nextAfterSeer();
                break;
            case PHASE_WITCH_TURN:
                phase = isRoleAlive(ROLE_GUARD) ? PHASE_GUARD_TURN : PHASE_DAY_START;
                break;
            case PHASE_GUARD_TURN:
                phase = PHASE_DAY_START;
                break;
            case PHASE_DAY_START:
                phase = PHASE_SPEAK;
                currentSpeaker = getFirstAliveSeat();
                break;
            case PHASE_SPEAK:
                phase = PHASE_VOTE;
                break;
            case PHASE_VOTE:
                phase = PHASE_NIGHT_START;
                day++;
                break;
            case PHASE_LAST_WORDS:
                // 遗言后检查胜负，然后继续
                if (checkWinner() != 0) {
                    phase = PHASE_GAME_OVER;
                } else {
                    phase = PHASE_NIGHT_START;
                    day++;
                }
                break;
        }
    }
    
    private int nextAfterSeer() {
        if (isRoleAlive(ROLE_WITCH)) {
            return PHASE_WITCH_TURN;
        } else if (isRoleAlive(ROLE_GUARD)) {
            return PHASE_GUARD_TURN;
        } else {
            return PHASE_DAY_START;
        }
    }
    
    /**
     * 获取第一个存活玩家座位
     */
    private int getFirstAliveSeat() {
        for (int i = 0; i < playerCount; i++) {
            if (alive[i]) return i;
        }
        return 0;
    }
    
    /**
     * 获取下一个发言者
     */
    public int getNextSpeaker() {
        for (int i = currentSpeaker + 1; i < playerCount; i++) {
            if (alive[i]) return i;
        }
        return -1;  // 发言结束
    }
    
    /**
     * 获取存活玩家数
     */
    public int getAliveCount() {
        int count = 0;
        for (boolean a : alive) {
            if (a) count++;
        }
        return count;
    }
}
