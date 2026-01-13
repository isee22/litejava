package game.werewolf;

import game.common.*;
import game.werewolf.vo.*;
import litejava.App;
import litejava.plugins.LiteJava;

import java.util.*;

/**
 * 狼人杀游戏服务器
 * 
 * 游戏流程:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                        游戏开始                              │
 * │                     分配角色并通知                           │
 * └─────────────────────────────────────────────────────────────┘
 *                              ↓
 * ┌─────────────────────────────────────────────────────────────┐
 * │                      【夜晚阶段】                            │
 * │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
 * │  │ 狼人杀人 │→│预言家查验│→│ 女巫用药 │→│ 守卫守护 │        │
 * │  └─────────┘  └─────────┘  └─────────┘  └─────────┘        │
 * └─────────────────────────────────────────────────────────────┘
 *                              ↓
 * ┌─────────────────────────────────────────────────────────────┐
 * │                      【白天阶段】                            │
 * │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
 * │  │公布死讯  │→│  遗言   │→│  发言   │→│  投票   │        │
 * │  └─────────┘  └─────────┘  └─────────┘  └─────────┘        │
 * └─────────────────────────────────────────────────────────────┘
 *                              ↓
 *                    检查胜负 → 未结束则回到夜晚
 */
public class WerewolfServer extends GameServer<WerewolfGame> {
    
    @Override
    protected String getGameName() { return "狼人杀"; }
    
    @Override
    protected void startGame(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = new WerewolfGame(room.seats.size());
        game.assignRoles();
        room.game = game;
        room.numOfGames++;
        
        // 通知每个玩家自己的角色
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                WwRoleVO roleVO = new WwRoleVO();
                roleVO.role = game.roles[seat.seatIndex];
                roleVO.roleName = WerewolfGame.getRoleName(roleVO.role);
                sendTo(seat.userId, WwCmd.ROLE_ASSIGN, roleVO);
                
                // 如果是狼人，告知同伴
                if (game.roles[seat.seatIndex] == WerewolfGame.ROLE_WOLF) {
                    WwWolfTeamVO teamVO = new WwWolfTeamVO();
                    teamVO.wolfSeats = game.getWolfSeats();
                    sendTo(seat.userId, WwCmd.WOLF_TEAMMATES, teamVO);
                }
            }
        }
        
        // 广播游戏开始
        WwGameStartVO startVO = new WwGameStartVO();
        startVO.playerCount = game.playerCount;
        startVO.day = game.day;
        broadcast(room, Cmd.GAME_START, startVO);
        
        // 进入夜晚
        enterNight(room);
        
        app.log.info("狼人杀游戏开始: 房间 " + room.id + ", " + game.playerCount + "人局");
    }
    
    /**
     * 进入夜晚阶段
     */
    private void enterNight(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        game.phase = WerewolfGame.PHASE_NIGHT_START;
        
        // 广播夜晚开始
        WwPhaseVO phaseVO = new WwPhaseVO();
        phaseVO.phase = game.phase;
        phaseVO.day = game.day;
        phaseVO.phaseName = "夜晚";
        broadcast(room, WwCmd.PHASE_CHANGE, phaseVO);
        
        // 进入狼人回合
        game.nextPhase();
        notifyWolfTurn(room);
    }
    
    /**
     * 通知狼人行动
     */
    private void notifyWolfTurn(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        
        WwPhaseVO phaseVO = new WwPhaseVO();
        phaseVO.phase = game.phase;
        phaseVO.day = game.day;
        phaseVO.phaseName = "狼人请睁眼";
        broadcast(room, WwCmd.PHASE_CHANGE, phaseVO);
        
        // 通知存活的狼人可以行动
        List<Integer> targets = new ArrayList<>();
        for (int i = 0; i < game.playerCount; i++) {
            if (game.alive[i] && game.roles[i] != WerewolfGame.ROLE_WOLF) {
                targets.add(i);
            }
        }
        
        for (int wolfSeat : game.getAliveWolfSeats()) {
            long userId = room.seats.get(wolfSeat).userId;
            WwTurnVO turnVO = new WwTurnVO();
            turnVO.action = "kill";
            turnVO.targets = targets;
            turnVO.timeout = 30;
            sendTo(userId, WwCmd.YOUR_TURN, turnVO);
        }
    }
    
    /**
     * 通知预言家行动
     */
    private void notifySeerTurn(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        
        WwPhaseVO phaseVO = new WwPhaseVO();
        phaseVO.phase = game.phase;
        phaseVO.day = game.day;
        phaseVO.phaseName = "预言家请睁眼";
        broadcast(room, WwCmd.PHASE_CHANGE, phaseVO);
        
        int seerSeat = game.getRoleSeat(WerewolfGame.ROLE_SEER);
        if (seerSeat >= 0 && game.alive[seerSeat]) {
            List<Integer> targets = new ArrayList<>();
            for (int i = 0; i < game.playerCount; i++) {
                if (game.alive[i] && i != seerSeat) {
                    targets.add(i);
                }
            }
            
            long userId = room.seats.get(seerSeat).userId;
            WwTurnVO turnVO = new WwTurnVO();
            turnVO.action = "check";
            turnVO.targets = targets;
            turnVO.timeout = 20;
            sendTo(userId, WwCmd.YOUR_TURN, turnVO);
        } else {
            // 预言家已死，跳过
            game.nextPhase();
            proceedNight(room);
        }
    }
    
    /**
     * 通知女巫行动
     */
    private void notifyWitchTurn(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        
        WwPhaseVO phaseVO = new WwPhaseVO();
        phaseVO.phase = game.phase;
        phaseVO.day = game.day;
        phaseVO.phaseName = "女巫请睁眼";
        broadcast(room, WwCmd.PHASE_CHANGE, phaseVO);
        
        int witchSeat = game.getRoleSeat(WerewolfGame.ROLE_WITCH);
        if (witchSeat >= 0 && game.alive[witchSeat]) {
            long userId = room.seats.get(witchSeat).userId;
            WwWitchInfoVO infoVO = new WwWitchInfoVO();
            infoVO.killedSeat = game.wolfKillTarget;
            infoVO.hasAntidote = game.hasAntidote;
            infoVO.hasPoison = game.hasPoison;
            
            // 毒药可选目标
            if (game.hasPoison) {
                infoVO.poisonTargets = new ArrayList<>();
                for (int i = 0; i < game.playerCount; i++) {
                    if (game.alive[i] && i != witchSeat) {
                        infoVO.poisonTargets.add(i);
                    }
                }
            }
            
            infoVO.timeout = 20;
            sendTo(userId, WwCmd.YOUR_TURN, infoVO);
        } else {
            game.nextPhase();
            proceedNight(room);
        }
    }
    
    /**
     * 通知守卫行动
     */
    private void notifyGuardTurn(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        
        WwPhaseVO phaseVO = new WwPhaseVO();
        phaseVO.phase = game.phase;
        phaseVO.day = game.day;
        phaseVO.phaseName = "守卫请睁眼";
        broadcast(room, WwCmd.PHASE_CHANGE, phaseVO);
        
        int guardSeat = game.getRoleSeat(WerewolfGame.ROLE_GUARD);
        if (guardSeat >= 0 && game.alive[guardSeat]) {
            List<Integer> targets = new ArrayList<>();
            for (int i = 0; i < game.playerCount; i++) {
                if (game.alive[i] && i != game.lastGuardTarget) {
                    targets.add(i);
                }
            }
            
            long userId = room.seats.get(guardSeat).userId;
            WwTurnVO turnVO = new WwTurnVO();
            turnVO.action = "protect";
            turnVO.targets = targets;
            turnVO.timeout = 15;
            sendTo(userId, WwCmd.YOUR_TURN, turnVO);
        } else {
            game.nextPhase();
            proceedNight(room);
        }
    }
    
    /**
     * 继续夜晚流程
     */
    private void proceedNight(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        
        switch (game.phase) {
            case WerewolfGame.PHASE_SEER_TURN:
                notifySeerTurn(room);
                break;
            case WerewolfGame.PHASE_WITCH_TURN:
                notifyWitchTurn(room);
                break;
            case WerewolfGame.PHASE_GUARD_TURN:
                notifyGuardTurn(room);
                break;
            case WerewolfGame.PHASE_DAY_START:
                enterDay(room);
                break;
        }
    }
    
    /**
     * 进入白天阶段
     */
    private void enterDay(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        
        // 结算夜晚死亡
        List<Integer> deaths = game.resolveNight();
        
        // 广播白天开始
        WwPhaseVO phaseVO = new WwPhaseVO();
        phaseVO.phase = game.phase;
        phaseVO.day = game.day;
        phaseVO.phaseName = "天亮了";
        broadcast(room, WwCmd.PHASE_CHANGE, phaseVO);
        
        // 公布死讯
        if (!deaths.isEmpty()) {
            WwDeathVO deathVO = new WwDeathVO();
            deathVO.deadSeats = deaths;
            deathVO.reason = "昨晚死亡";
            broadcast(room, WwCmd.DEATH_ANNOUNCE, deathVO);
            
            // 检查胜负
            int winner = game.checkWinner();
            if (winner != 0) {
                endGame(room, winner);
                return;
            }
            
            // 检查猎人是否死亡且可以开枪
            for (int deadSeat : deaths) {
                if (game.roles[deadSeat] == WerewolfGame.ROLE_HUNTER && game.hunterCanShoot) {
                    notifyHunterShoot(room, deadSeat);
                    return;
                }
            }
        } else {
            // 平安夜
            WwDeathVO deathVO = new WwDeathVO();
            deathVO.deadSeats = new ArrayList<>();
            deathVO.reason = "平安夜";
            broadcast(room, WwCmd.DEATH_ANNOUNCE, deathVO);
        }
        
        // 进入发言阶段
        game.nextPhase();
        enterSpeak(room);
    }
    
    /**
     * 进入发言阶段
     */
    private void enterSpeak(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        
        WwPhaseVO phaseVO = new WwPhaseVO();
        phaseVO.phase = game.phase;
        phaseVO.day = game.day;
        phaseVO.phaseName = "发言阶段";
        phaseVO.currentSpeaker = game.currentSpeaker;
        broadcast(room, WwCmd.PHASE_CHANGE, phaseVO);
        
        // 通知当前发言者
        if (game.currentSpeaker >= 0) {
            long userId = room.seats.get(game.currentSpeaker).userId;
            WwTurnVO turnVO = new WwTurnVO();
            turnVO.action = "speak";
            turnVO.timeout = 60;
            sendTo(userId, WwCmd.YOUR_TURN, turnVO);
        }
    }
    
    /**
     * 进入投票阶段
     */
    private void enterVote(RoomMgr.Room<WerewolfGame> room) {
        WerewolfGame game = room.game;
        game.phase = WerewolfGame.PHASE_VOTE;
        
        WwPhaseVO phaseVO = new WwPhaseVO();
        phaseVO.phase = game.phase;
        phaseVO.day = game.day;
        phaseVO.phaseName = "投票阶段";
        broadcast(room, WwCmd.PHASE_CHANGE, phaseVO);
        
        // 通知所有存活玩家投票
        List<Integer> targets = new ArrayList<>();
        for (int i = 0; i < game.playerCount; i++) {
            if (game.alive[i]) {
                targets.add(i);
            }
        }
        
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0 && game.alive[seat.seatIndex]) {
                WwTurnVO turnVO = new WwTurnVO();
                turnVO.action = "vote";
                turnVO.targets = targets;
                turnVO.timeout = 30;
                sendTo(seat.userId, WwCmd.YOUR_TURN, turnVO);
            }
        }
    }
    
    /**
     * 通知猎人开枪
     */
    private void notifyHunterShoot(RoomMgr.Room<WerewolfGame> room, int hunterSeat) {
        WerewolfGame game = room.game;
        
        List<Integer> targets = new ArrayList<>();
        for (int i = 0; i < game.playerCount; i++) {
            if (game.alive[i]) {
                targets.add(i);
            }
        }
        
        long userId = room.seats.get(hunterSeat).userId;
        WwTurnVO turnVO = new WwTurnVO();
        turnVO.action = "shoot";
        turnVO.targets = targets;
        turnVO.timeout = 15;
        sendTo(userId, WwCmd.YOUR_TURN, turnVO);
    }
    
    /**
     * 结束游戏
     */
    private void endGame(RoomMgr.Room<WerewolfGame> room, int winner) {
        WerewolfGame game = room.game;
        game.phase = WerewolfGame.PHASE_GAME_OVER;
        game.winner = winner;
        
        // 公布所有角色
        WwGameOverVO overVO = new WwGameOverVO();
        overVO.winner = winner;
        overVO.winnerName = winner == 1 ? "好人阵营" : "狼人阵营";
        overVO.roles = new HashMap<>();
        for (int i = 0; i < game.playerCount; i++) {
            overVO.roles.put(i, game.roles[i]);
        }
        broadcast(room, WwCmd.GAME_OVER, overVO);
        
        // 调用基类结束处理
        onGameOver(room);
        
        app.log.info("狼人杀游戏结束: 房间 " + room.id + ", " + overVO.winnerName + "胜利");
    }
    
    @Override
    protected void onGameCmd(long userId, RoomMgr.Room<WerewolfGame> room, int cmd, Map<String, Object> data) {
        WerewolfGame game = room.game;
        if (game == null) {
            GameException.error(ErrCode.GAME_NOT_STARTED);
        }
        
        int seatIndex = roomMgr.getUserSeat(userId);
        
        switch (cmd) {
            case WwCmd.WOLF_KILL:
                doWolfKill(room, game, seatIndex, data);
                break;
            case WwCmd.SEER_CHECK:
                doSeerCheck(room, game, seatIndex, data);
                break;
            case WwCmd.WITCH_USE:
                doWitchUse(room, game, seatIndex, data);
                break;
            case WwCmd.GUARD_PROTECT:
                doGuardProtect(room, game, seatIndex, data);
                break;
            case WwCmd.HUNTER_SHOOT:
                doHunterShoot(room, game, seatIndex, data);
                break;
            case WwCmd.SPEAK:
                doSpeak(room, game, seatIndex, data, userId);
                break;
            case WwCmd.VOTE:
                doVote(room, game, seatIndex, data);
                break;
            default:
                GameException.error(ErrCode.INVALID_ACTION);
        }
    }
    
    private void doWolfKill(RoomMgr.Room<WerewolfGame> room, WerewolfGame game, int seatIndex, Map<String, Object> data) {
        if (game.phase != WerewolfGame.PHASE_WOLF_TURN) {
            GameException.error(WwErr.WRONG_PHASE);
        }
        if (game.roles[seatIndex] != WerewolfGame.ROLE_WOLF) {
            GameException.error(WwErr.NOT_YOUR_ROLE);
        }
        if (!game.alive[seatIndex]) {
            GameException.error(WwErr.YOU_ARE_DEAD);
        }
        
        int targetSeat = ((Number) data.getOrDefault("targetSeat", -1)).intValue();
        
        // 狼人投票
        boolean allVoted = game.wolfVote(seatIndex, targetSeat);
        
        // 通知其他狼人
        WwWolfKillVO killVO = new WwWolfKillVO();
        killVO.wolfSeat = seatIndex;
        killVO.targetSeat = targetSeat;
        killVO.confirmed = allVoted;
        if (allVoted) {
            killVO.finalTarget = game.wolfKillTarget;
        }
        
        for (int wolfSeat : game.getAliveWolfSeats()) {
            long wolfUserId = room.seats.get(wolfSeat).userId;
            sendTo(wolfUserId, WwCmd.WOLF_KILL_RESULT, killVO);
        }
        
        // 所有狼人投票完成，进入下一阶段
        if (allVoted) {
            game.nextPhase();
            proceedNight(room);
        }
    }
    
    private void doSeerCheck(RoomMgr.Room<WerewolfGame> room, WerewolfGame game, int seatIndex, Map<String, Object> data) {
        if (game.phase != WerewolfGame.PHASE_SEER_TURN) {
            GameException.error(WwErr.WRONG_PHASE);
        }
        if (game.roles[seatIndex] != WerewolfGame.ROLE_SEER) {
            GameException.error(WwErr.NOT_YOUR_ROLE);
        }
        
        int targetSeat = ((Number) data.get("targetSeat")).intValue();
        boolean isWolf = game.seerCheck(targetSeat);
        
        // 只通知预言家
        long userId = room.seats.get(seatIndex).userId;
        WwSeerResultVO resultVO = new WwSeerResultVO();
        resultVO.targetSeat = targetSeat;
        resultVO.isWolf = isWolf;
        sendTo(userId, WwCmd.SEER_CHECK_RESULT, resultVO);
        
        // 进入下一阶段
        game.nextPhase();
        proceedNight(room);
    }
    
    private void doWitchUse(RoomMgr.Room<WerewolfGame> room, WerewolfGame game, int seatIndex, Map<String, Object> data) {
        if (game.phase != WerewolfGame.PHASE_WITCH_TURN) {
            GameException.error(WwErr.WRONG_PHASE);
        }
        if (game.roles[seatIndex] != WerewolfGame.ROLE_WITCH) {
            GameException.error(WwErr.NOT_YOUR_ROLE);
        }
        
        boolean useAntidote = (Boolean) data.getOrDefault("useAntidote", false);
        boolean usePoison = (Boolean) data.getOrDefault("usePoison", false);
        int poisonTarget = ((Number) data.getOrDefault("poisonTarget", -1)).intValue();
        
        if (useAntidote && !game.hasAntidote) {
            GameException.error(WwErr.NO_ANTIDOTE);
        }
        if (usePoison && !game.hasPoison) {
            GameException.error(WwErr.NO_POISON);
        }
        
        game.witchUse(useAntidote, usePoison, poisonTarget);
        
        // 通知女巫
        long userId = room.seats.get(seatIndex).userId;
        WwWitchResultVO resultVO = new WwWitchResultVO();
        resultVO.antidoteUsed = useAntidote;
        resultVO.poisonUsed = usePoison;
        sendTo(userId, WwCmd.WITCH_USE_RESULT, resultVO);
        
        // 进入下一阶段
        game.nextPhase();
        proceedNight(room);
    }
    
    private void doGuardProtect(RoomMgr.Room<WerewolfGame> room, WerewolfGame game, int seatIndex, Map<String, Object> data) {
        if (game.phase != WerewolfGame.PHASE_GUARD_TURN) {
            GameException.error(WwErr.WRONG_PHASE);
        }
        if (game.roles[seatIndex] != WerewolfGame.ROLE_GUARD) {
            GameException.error(WwErr.NOT_YOUR_ROLE);
        }
        
        int targetSeat = ((Number) data.getOrDefault("targetSeat", -1)).intValue();
        
        if (targetSeat == game.lastGuardTarget) {
            GameException.error(WwErr.SAME_GUARD_TARGET);
        }
        
        game.guardProtect(targetSeat);
        
        // 通知守卫
        long userId = room.seats.get(seatIndex).userId;
        WwGuardResultVO resultVO = new WwGuardResultVO();
        resultVO.targetSeat = targetSeat;
        sendTo(userId, WwCmd.GUARD_PROTECT_RESULT, resultVO);
        
        // 进入下一阶段
        game.nextPhase();
        proceedNight(room);
    }
    
    private void doHunterShoot(RoomMgr.Room<WerewolfGame> room, WerewolfGame game, int seatIndex, Map<String, Object> data) {
        if (game.roles[seatIndex] != WerewolfGame.ROLE_HUNTER) {
            GameException.error(WwErr.NOT_YOUR_ROLE);
        }
        if (!game.hunterCanShoot) {
            GameException.error(WwErr.NO_POISON);  // 被毒死不能开枪
        }
        
        int targetSeat = ((Number) data.getOrDefault("targetSeat", -1)).intValue();
        
        if (targetSeat >= 0) {
            game.hunterShoot(targetSeat);
            
            // 广播猎人开枪
            WwHunterShootVO shootVO = new WwHunterShootVO();
            shootVO.hunterSeat = seatIndex;
            shootVO.targetSeat = targetSeat;
            broadcast(room, WwCmd.HUNTER_SHOOT_RESULT, shootVO);
            
            // 检查胜负
            int winner = game.checkWinner();
            if (winner != 0) {
                endGame(room, winner);
                return;
            }
        }
        
        // 继续游戏流程
        game.nextPhase();
        enterSpeak(room);
    }
    
    private void doSpeak(RoomMgr.Room<WerewolfGame> room, WerewolfGame game, int seatIndex, Map<String, Object> data, long userId) {
        if (game.phase != WerewolfGame.PHASE_SPEAK) {
            GameException.error(WwErr.WRONG_PHASE);
        }
        if (seatIndex != game.currentSpeaker) {
            GameException.error(WwErr.NOT_YOUR_TURN);
        }
        
        String content = (String) data.getOrDefault("content", "");
        
        // 广播发言
        WwSpeakVO speakVO = new WwSpeakVO();
        speakVO.seatIndex = seatIndex;
        speakVO.userId = userId;
        speakVO.content = content;
        broadcast(room, WwCmd.SPEAK_BROADCAST, speakVO);
        
        // 下一个发言者
        int nextSpeaker = game.getNextSpeaker();
        if (nextSpeaker >= 0) {
            game.currentSpeaker = nextSpeaker;
            enterSpeak(room);
        } else {
            // 发言结束，进入投票
            enterVote(room);
        }
    }
    
    private void doVote(RoomMgr.Room<WerewolfGame> room, WerewolfGame game, int seatIndex, Map<String, Object> data) {
        if (game.phase != WerewolfGame.PHASE_VOTE) {
            GameException.error(WwErr.WRONG_PHASE);
        }
        if (!game.alive[seatIndex]) {
            GameException.error(WwErr.YOU_ARE_DEAD);
        }
        if (game.votes.containsKey(seatIndex)) {
            GameException.error(WwErr.ALREADY_VOTED);
        }
        
        int targetSeat = ((Number) data.getOrDefault("targetSeat", -1)).intValue();
        game.vote(seatIndex, targetSeat);
        
        // 检查是否所有人都投票了
        int aliveCount = game.getAliveCount();
        if (game.votes.size() >= aliveCount) {
            // 结算投票
            int eliminated = game.resolveVote();
            
            WwVoteResultVO resultVO = new WwVoteResultVO();
            resultVO.votes = new HashMap<>(game.votes);
            resultVO.eliminatedSeat = eliminated;
            broadcast(room, WwCmd.VOTE_RESULT, resultVO);
            
            if (eliminated >= 0) {
                // 检查胜负
                int winner = game.checkWinner();
                if (winner != 0) {
                    endGame(room, winner);
                    return;
                }
                
                // 检查猎人
                if (game.roles[eliminated] == WerewolfGame.ROLE_HUNTER && game.hunterCanShoot) {
                    notifyHunterShoot(room, eliminated);
                    return;
                }
            }
            
            // 进入夜晚
            game.day++;
            enterNight(room);
        }
    }
    
    @Override
    protected Object getGameState(RoomMgr.Room<WerewolfGame> room, int seatIndex) {
        if (room.game == null) return null;
        
        WerewolfGame game = room.game;
        WwStateVO vo = new WwStateVO();
        vo.day = game.day;
        vo.phase = game.phase;
        vo.myRole = game.roles[seatIndex];
        vo.alive = game.alive.clone();
        
        // 狼人可以看到同伴
        if (game.roles[seatIndex] == WerewolfGame.ROLE_WOLF) {
            vo.wolfSeats = game.getWolfSeats();
        }
        
        return vo;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new WerewolfServer().start(app);
    }
}
