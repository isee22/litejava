package game.gobang;

import game.common.*;
import game.gobang.vo.*;
import litejava.App;
import litejava.plugins.LiteJava;

import java.util.Map;

/**
 * 五子棋游戏服务器
 */
public class GobangServer extends GameServer<GobangGame> {
    
    @Override
    protected String getGameName() { return "五子棋"; }
    
    @Override
    protected void startGame(RoomMgr.Room<GobangGame> room) {
        long[] players = new long[2];
        int idx = 0;
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0 && idx < 2) {
                players[idx++] = seat.userId;
            }
        }
        
        room.game = new GobangGame(players[0], players[1]);
        room.numOfGames++;
        
        GbGameStartVO vo = new GbGameStartVO();
        vo.black = players[0];
        vo.white = players[1];
        vo.currentPlayer = players[0];
        broadcast(room, Cmd.GAME_START, vo);
        
        app.log.info("游戏开始: 房间 " + room.id);
    }
    
    @Override
    protected void onGameCmd(long userId, RoomMgr.Room<GobangGame> room, int cmd, Map<String, Object> data) {
        GobangGame game = room.game;
        if (game == null) {
            GameException.error(ErrCode.GAME_NOT_STARTED);
        }
        if (game.isOver) {
            GameException.error(ErrCode.GAME_OVER);
        }
        
        if (cmd == GbCmd.MOVE) {
            doMove(room, game, userId, data);
        } else {
            GameException.error(ErrCode.INVALID_ACTION);
        }
    }
    
    private void doMove(RoomMgr.Room<GobangGame> room, GobangGame game, long userId, Map<String, Object> data) {
        int row = ((Number) data.get("row")).intValue();
        int col = ((Number) data.get("col")).intValue();
        
        int result = game.move(userId, row, col);
        if (result == 1) {
            GameException.error(ErrCode.NOT_YOUR_TURN);
        }
        if (result == 2) {
            GameException.error(ErrCode.INVALID_ACTION);
        }
        
        int playerIndex = game.getPlayerIndex(userId);
        GbMoveVO moveVO = new GbMoveVO();
        moveVO.userId = userId;
        moveVO.row = row;
        moveVO.col = col;
        moveVO.stone = playerIndex == 0 ? 1 : 2;
        broadcast(room, GbCmd.MOVE_RESULT, moveVO);
        
        if (game.isOver) {
            GbGameOverVO overVO = new GbGameOverVO();
            overVO.winner = game.winner;
            if (game.winner < 2) {
                overVO.winnerId = game.players[game.winner];
            }
            broadcast(room, Cmd.GAME_OVER, overVO);
            onGameOver(room);
        } else {
            GbTurnVO turnVO = new GbTurnVO();
            turnVO.currentPlayer = game.getCurrentPlayer();
            broadcast(room, Cmd.TURN, turnVO);
        }
    }
    
    @Override
    protected Object getGameState(RoomMgr.Room<GobangGame> room, int seatIndex) {
        if (room.game == null) return null;
        
        GobangGame game = room.game;
        GbStateVO vo = new GbStateVO();
        vo.board = game.board;
        vo.currentPlayer = game.getCurrentPlayer();
        vo.isOver = game.isOver;
        vo.winner = game.winner;
        return vo;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new GobangServer().start(app);
    }
}
