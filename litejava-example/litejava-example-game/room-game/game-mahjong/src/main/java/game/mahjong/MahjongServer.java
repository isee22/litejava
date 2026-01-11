package game.mahjong;

import game.common.*;
import game.mahjong.vo.*;
import litejava.App;
import litejava.plugins.LiteJava;

import java.util.*;

/**
 * 麻将游戏服务器 (四川麻将)
 */
public class MahjongServer extends GameServer<MahjongGame> {
    
    @Override
    protected String getGameName() { return "麻将"; }
    
    @Override
    protected void startGame(RoomMgr.Room<MahjongGame> room) {
        MahjongGame game = new MahjongGame(room.seats.size());
        game.deal();
        room.game = game;
        room.numOfGames++;
        
        MjGameStartVO startVO = new MjGameStartVO();
        startVO.numOfGames = room.numOfGames;
        startVO.dealerSeat = game.dealerSeat;
        startVO.currentSeat = game.currentSeat;
        startVO.remaining = game.getRemainingCards();
        broadcast(room, Cmd.GAME_START, startVO);
        
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                MjDealVO dealVO = new MjDealVO();
                dealVO.cards = game.getPlayerHand(seat.seatIndex);
                sendTo(seat.userId, Cmd.DEAL, dealVO);
            }
        }
        
        app.log.info("麻将游戏开始: 房间 " + room.id);
    }
    
    @Override
    protected void onGameCmd(long userId, RoomMgr.Room<MahjongGame> room, int cmd, Map<String, Object> data) {
        MahjongGame game = room.game;
        if (game == null) {
            GameException.error(ErrCode.GAME_NOT_STARTED);
        }
        if (game.status != MahjongGame.STATUS_PLAYING) {
            GameException.error(ErrCode.GAME_OVER);
        }
        
        int seatIndex = roomMgr.getUserSeat(userId);
        
        switch (cmd) {
            case MjCmd.DISCARD:
                doDiscard(room, game, seatIndex, data);
                break;
            case MjCmd.PENG:
                doPeng(room, game, seatIndex);
                break;
            case MjCmd.GANG:
                doGang(room, game, seatIndex, data);
                break;
            case MjCmd.HU:
                doHu(room, game, seatIndex, data);
                break;
            case MjCmd.PASS:
                doPass(room, game, seatIndex);
                break;
            default:
                GameException.error(ErrCode.INVALID_ACTION);
        }
    }

    
    private void doDiscard(RoomMgr.Room<MahjongGame> room, MahjongGame game, int seatIndex, Map<String, Object> data) {
        if (seatIndex != game.currentSeat) {
            GameException.error(ErrCode.NOT_YOUR_TURN);
        }
        
        int card = ((Number) data.get("card")).intValue();
        MjDiscardResultVO result = game.discard(seatIndex, card);
        if (result == null) {
            GameException.error(ErrCode.INVALID_CARDS);
        }
        
        broadcast(room, MjCmd.DISCARD_RESULT, result);
        
        if (result.nextSeat >= 0) {
            int drawn = game.draw(result.nextSeat);
            if (drawn >= 0) {
                long nextUserId = getSeatUserId(room, result.nextSeat);
                MjDrawVO drawVO = new MjDrawVO();
                drawVO.card = drawn;
                drawVO.remaining = game.getRemainingCards();
                sendTo(nextUserId, Cmd.DRAW, drawVO);
            }
        }
    }
    
    private void doPeng(RoomMgr.Room<MahjongGame> room, MahjongGame game, int seatIndex) {
        MjPengResultVO result = game.peng(seatIndex);
        if (result == null) {
            GameException.error(MjErr.CANNOT_PENG);
        }
        broadcast(room, MjCmd.PENG_RESULT, result);
    }
    
    private void doGang(RoomMgr.Room<MahjongGame> room, MahjongGame game, int seatIndex, Map<String, Object> data) {
        int gangCard = ((Number) data.get("card")).intValue();
        boolean isSelf = (Boolean) data.getOrDefault("self", false);
        MjGangResultVO result = game.gang(seatIndex, gangCard, isSelf);
        if (result == null) {
            GameException.error(MjErr.CANNOT_GANG);
        }
        broadcast(room, MjCmd.GANG_RESULT, result);
    }
    
    private void doHu(RoomMgr.Room<MahjongGame> room, MahjongGame game, int seatIndex, Map<String, Object> data) {
        boolean isSelfDrawn = (Boolean) data.getOrDefault("selfDrawn", false);
        MjHuResultVO result = game.hu(seatIndex, isSelfDrawn);
        if (result == null) {
            GameException.error(MjErr.CANNOT_HU);
        }
        broadcast(room, MjCmd.HU_RESULT, result);
        if (result.gameOver) {
            onGameOver(room);
        }
    }
    
    private void doPass(RoomMgr.Room<MahjongGame> room, MahjongGame game, int seatIndex) {
        game.pass(seatIndex);
        int nextSeat = game.currentSeat;
        int drawn = game.draw(nextSeat);
        if (drawn >= 0) {
            long nextUserId = getSeatUserId(room, nextSeat);
            MjDrawVO drawVO = new MjDrawVO();
            drawVO.card = drawn;
            drawVO.remaining = game.getRemainingCards();
            sendTo(nextUserId, Cmd.DRAW, drawVO);
            
            MjTurnVO turnVO = new MjTurnVO();
            turnVO.currentSeat = nextSeat;
            broadcast(room, Cmd.TURN, turnVO);
        }
    }
    
    @Override
    protected Object getGameState(RoomMgr.Room<MahjongGame> room, int seatIndex) {
        if (room.game == null) return null;
        
        MahjongGame game = room.game;
        MjStateVO vo = new MjStateVO();
        vo.status = game.status;
        vo.currentSeat = game.currentSeat;
        vo.dealerSeat = game.dealerSeat;
        vo.myHand = game.getPlayerHand(seatIndex);
        vo.remaining = game.getRemainingCards();
        vo.lastDiscard = game.lastDiscard;
        vo.lastDiscardSeat = game.lastDiscardSeat;
        return vo;
    }
    
    private long getSeatUserId(RoomMgr.Room<MahjongGame> room, int seatIndex) {
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.seatIndex == seatIndex) {
                return seat.userId;
            }
        }
        return 0;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new MahjongServer().start(app);
    }
}
