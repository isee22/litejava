package game.moba;

import game.common.*;
import game.moba.vo.*;
import litejava.App;
import litejava.plugins.LiteJava;

import java.util.*;

/**
 * MOBA 游戏服务器 (简化版 5v5)
 */
public class MobaServer extends GameServer<MobaGame> {
    
    @Override
    protected String getGameName() { return "MOBA"; }
    
    @Override
    protected void startGame(RoomMgr.Room<MobaGame> room) {
        long[] blue = new long[5];
        long[] red = new long[5];
        int bi = 0, ri = 0;
        
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                if (seat.seatIndex < 5) {
                    blue[bi++] = seat.userId;
                } else {
                    red[ri++] = seat.userId;
                }
            }
        }
        
        MobaGame game = new MobaGame(
            Arrays.copyOf(blue, bi),
            Arrays.copyOf(red, ri)
        );
        room.game = game;
        room.numOfGames++;
        
        MobaGameStartVO startVO = new MobaGameStartVO();
        startVO.blueTeam = game.blueTeam;
        startVO.redTeam = game.redTeam;
        startVO.mapWidth = MobaGame.MAP_WIDTH;
        startVO.mapHeight = MobaGame.MAP_HEIGHT;
        broadcast(room, Cmd.GAME_START, startVO);
        
        game.start(() -> {
            Map<String, Object> frame = game.getFrameState();
            broadcast(room, MobaCmd.FRAME, frame);
            
            if (game.isOver) {
                MobaGameOverVO overVO = new MobaGameOverVO();
                overVO.winner = game.winner;
                broadcast(room, Cmd.GAME_OVER, overVO);
                onGameOver(room);
            }
        });
        
        app.log.info("MOBA 游戏开始: 房间 " + room.id);
    }

    
    @Override
    protected void onGameCmd(long userId, RoomMgr.Room<MobaGame> room, int cmd, Map<String, Object> data) {
        MobaGame game = room.game;
        if (game == null) {
            GameException.error(ErrCode.GAME_NOT_STARTED);
        }
        if (game.isOver) {
            GameException.error(ErrCode.GAME_OVER);
        }
        
        switch (cmd) {
            case MobaCmd.MOVE:
                doMove(game, userId, data);
                break;
            case MobaCmd.ATTACK:
                doAttack(room, game, userId, data);
                break;
            case MobaCmd.SKILL:
                doSkill(room, game, userId, data);
                break;
            case MobaCmd.ATTACK_BASE:
                doAttackBase(room, game, userId);
                break;
            default:
                GameException.error(ErrCode.INVALID_ACTION);
        }
    }
    
    private void doMove(MobaGame game, long userId, Map<String, Object> data) {
        double x = ((Number) data.get("x")).doubleValue();
        double y = ((Number) data.get("y")).doubleValue();
        game.move(userId, x, y);
    }
    
    private void doAttack(RoomMgr.Room<MobaGame> room, MobaGame game, long userId, Map<String, Object> data) {
        long targetId = ((Number) data.get("targetId")).longValue();
        Map<String, Object> atkResult = game.attack(userId, targetId);
        if (atkResult != null) {
            broadcast(room, MobaCmd.ATTACK, atkResult);
        }
    }
    
    private void doSkill(RoomMgr.Room<MobaGame> room, MobaGame game, long userId, Map<String, Object> data) {
        double sx = ((Number) data.get("x")).doubleValue();
        double sy = ((Number) data.get("y")).doubleValue();
        List<Map<String, Object>> hits = game.skill(userId, sx, sy);
        if (hits != null && !hits.isEmpty()) {
            MobaSkillVO skillVO = new MobaSkillVO();
            skillVO.userId = userId;
            skillVO.x = sx;
            skillVO.y = sy;
            skillVO.hits = hits;
            broadcast(room, MobaCmd.SKILL, skillVO);
        }
    }
    
    private void doAttackBase(RoomMgr.Room<MobaGame> room, MobaGame game, long userId) {
        Map<String, Object> baseResult = game.attackBase(userId);
        if (baseResult != null) {
            broadcast(room, MobaCmd.ATTACK_BASE, baseResult);
        }
    }
    
    @Override
    protected Object getGameState(RoomMgr.Room<MobaGame> room, int seatIndex) {
        if (room.game == null) return null;
        return room.game.getFrameState();
    }
    
    @Override
    protected void onGameOver(RoomMgr.Room<MobaGame> room) {
        if (room.game != null) {
            room.game.stop();
        }
        super.onGameOver(room);
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new MobaServer().start(app);
    }
}
