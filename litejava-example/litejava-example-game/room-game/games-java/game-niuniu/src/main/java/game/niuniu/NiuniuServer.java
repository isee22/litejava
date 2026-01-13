package game.niuniu;

import game.common.*;
import game.niuniu.vo.*;
import litejava.App;
import litejava.plugins.LiteJava;

import java.util.*;

/**
 * 牛牛游戏服务器
 */
public class NiuniuServer extends GameServer<NiuniuGame> {
    
    @Override
    protected String getGameName() { return "牛牛"; }
    
    @Override
    protected void startGame(RoomMgr.Room<NiuniuGame> room) {
        int playerCount = 0;
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) playerCount++;
        }
        
        NiuniuGame game = new NiuniuGame(playerCount);
        game.deal();
        room.game = game;
        room.numOfGames++;
        
        NnGameStartVO startVO = new NnGameStartVO();
        startVO.numOfGames = room.numOfGames;
        startVO.bankerSeat = game.bankerSeat;
        broadcast(room, Cmd.GAME_START, startVO);
        
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                NnDealVO dealVO = new NnDealVO();
                dealVO.cards = game.getFirstFourCards(seat.seatIndex);
                dealVO.isBanker = seat.seatIndex == game.bankerSeat;
                sendTo(seat.userId, Cmd.DEAL, dealVO);
            }
        }
        
        app.log.info("牛牛游戏开始: 房间 " + room.id);
    }
    
    @Override
    protected void onGameCmd(long userId, RoomMgr.Room<NiuniuGame> room, int cmd, Map<String, Object> data) {
        NiuniuGame game = room.game;
        if (game == null) {
            GameException.error(ErrCode.GAME_NOT_STARTED);
        }
        if (game.isOver) {
            GameException.error(ErrCode.GAME_OVER);
        }
        
        int seatIndex = roomMgr.getUserSeat(userId);
        
        switch (cmd) {
            case NnCmd.BET:
                doBet(room, game, seatIndex, data);
                break;
            case NnCmd.SHOW:
                doShow(room, game, seatIndex);
                break;
            default:
                GameException.error(ErrCode.INVALID_ACTION);
        }
    }

    
    private void doBet(RoomMgr.Room<NiuniuGame> room, NiuniuGame game, int seatIndex, Map<String, Object> data) {
        if (game.phase != NiuniuGame.PHASE_BET) {
            GameException.error(NnErr.NOT_BET_PHASE);
        }
        if (seatIndex == game.bankerSeat) {
            GameException.error(NnErr.BANKER_CANNOT_BET);
        }
        if (game.bets[seatIndex] > 0) {
            GameException.error(NnErr.ALREADY_BET);
        }
        
        int multiple = ((Number) data.get("multiple")).intValue();
        Map<String, Object> betResult = game.bet(seatIndex, multiple);
        if (betResult == null) {
            GameException.error(ErrCode.INVALID_ACTION);
        }
        
        broadcast(room, NnCmd.BET_RESULT, betResult);
        
        if (game.allBet()) {
            for (RoomMgr.Seat seat : room.seats) {
                if (seat.userId > 0) {
                    NnFifthCardVO fifthVO = new NnFifthCardVO();
                    fifthVO.card = game.getFifthCard(seat.seatIndex);
                    sendTo(seat.userId, NnCmd.FIFTH_CARD, fifthVO);
                }
            }
        }
    }
    
    private void doShow(RoomMgr.Room<NiuniuGame> room, NiuniuGame game, int seatIndex) {
        if (game.phase != NiuniuGame.PHASE_SHOW) {
            GameException.error(NnErr.NOT_SHOW_PHASE);
        }
        
        Map<String, Object> showResult = game.show(seatIndex);
        if (showResult == null) {
            GameException.error(ErrCode.INVALID_ACTION);
        }
        
        broadcast(room, NnCmd.SHOW_RESULT, showResult);
        
        if (game.allShown()) {
            Map<String, Object> settleResult = game.settle();
            broadcast(room, NnCmd.SETTLE, settleResult);
            onGameOver(room);
        }
    }
    
    @Override
    protected Object getGameState(RoomMgr.Room<NiuniuGame> room, int seatIndex) {
        if (room.game == null) return null;
        
        NiuniuGame game = room.game;
        NnStateVO vo = new NnStateVO();
        vo.phase = game.phase;
        vo.bankerSeat = game.bankerSeat;
        vo.myCards = game.getCards(seatIndex);
        vo.myBet = game.bets[seatIndex];
        return vo;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new NiuniuServer().start(app);
    }
}
