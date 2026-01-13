package game.texas;

import game.common.*;
import game.texas.vo.*;
import litejava.App;
import litejava.plugins.LiteJava;

import java.util.*;

/**
 * 德州扑克游戏服务器
 */
public class TexasServer extends GameServer<TexasGame> {
    
    private static final int START_CHIPS = 1000;
    
    @Override
    protected String getGameName() { return "德州扑克"; }
    
    @Override
    protected void startGame(RoomMgr.Room<TexasGame> room) {
        int playerCount = 0;
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) playerCount++;
        }
        
        TexasGame game = new TexasGame(playerCount, START_CHIPS);
        game.deal();
        room.game = game;
        room.numOfGames++;
        
        TxGameStartVO startVO = new TxGameStartVO();
        startVO.numOfGames = room.numOfGames;
        startVO.dealerSeat = game.dealerSeat;
        startVO.smallBlindSeat = game.smallBlindSeat;
        startVO.bigBlindSeat = game.bigBlindSeat;
        startVO.currentSeat = game.currentSeat;
        startVO.pot = game.pot;
        startVO.smallBlind = game.smallBlind;
        startVO.bigBlind = game.bigBlind;
        broadcast(room, Cmd.GAME_START, startVO);
        
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                TxDealVO dealVO = new TxDealVO();
                dealVO.cards = game.getHoleCards(seat.seatIndex);
                dealVO.chips = game.chips[seat.seatIndex];
                sendTo(seat.userId, Cmd.DEAL, dealVO);
            }
        }
        
        app.log.info("德州扑克开始: 房间 " + room.id);
    }

    
    @Override
    protected void onGameCmd(long userId, RoomMgr.Room<TexasGame> room, int cmd, Map<String, Object> data) {
        TexasGame game = room.game;
        if (game == null) {
            GameException.error(ErrCode.GAME_NOT_STARTED);
        }
        if (game.isOver) {
            GameException.error(ErrCode.GAME_OVER);
        }
        
        int seatIndex = roomMgr.getUserSeat(userId);
        if (seatIndex != game.currentSeat) {
            GameException.error(ErrCode.NOT_YOUR_TURN);
        }
        
        Map<String, Object> result = null;
        
        switch (cmd) {
            case TxCmd.CALL:
                result = game.call(seatIndex);
                break;
            case TxCmd.RAISE:
                int amount = ((Number) data.get("amount")).intValue();
                result = game.raise(seatIndex, amount);
                if (result == null) {
                    GameException.error(TxErr.INVALID_RAISE);
                }
                break;
            case TxCmd.CHECK:
                result = game.check(seatIndex);
                if (result == null) {
                    GameException.error(TxErr.CANNOT_CHECK);
                }
                break;
            case TxCmd.FOLD:
                result = game.fold(seatIndex);
                break;
            case TxCmd.ALLIN:
                result = game.allIn(seatIndex);
                break;
            default:
                GameException.error(ErrCode.INVALID_ACTION);
        }
        
        if (result == null) {
            GameException.error(ErrCode.INVALID_ACTION);
        }
        
        broadcast(room, TxCmd.ACTION_RESULT, result);
        
        if (game.stage == TexasGame.STAGE_FLOP || 
            game.stage == TexasGame.STAGE_TURN || 
            game.stage == TexasGame.STAGE_RIVER) {
            TxCommunityVO communityVO = new TxCommunityVO();
            communityVO.stage = game.stage;
            communityVO.cards = game.getCommunityCards();
            communityVO.pot = game.pot;
            broadcast(room, TxCmd.COMMUNITY, communityVO);
        }
        
        if (game.isOver) {
            TxGameOverVO overVO = new TxGameOverVO();
            overVO.winner = game.winner;
            overVO.pot = game.pot;
            overVO.chips = game.chips;
            broadcast(room, Cmd.GAME_OVER, overVO);
            onGameOver(room);
        }
    }
    
    @Override
    protected Object getGameState(RoomMgr.Room<TexasGame> room, int seatIndex) {
        if (room.game == null) return null;
        
        TexasGame game = room.game;
        TxStateVO vo = new TxStateVO();
        vo.stage = game.stage;
        vo.currentSeat = game.currentSeat;
        vo.pot = game.pot;
        vo.currentBet = game.currentBet;
        vo.myCards = game.getHoleCards(seatIndex);
        vo.myChips = game.chips[seatIndex];
        vo.myBet = game.bets[seatIndex];
        vo.communityCards = game.getCommunityCards();
        return vo;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new TexasServer().start(app);
    }
}
