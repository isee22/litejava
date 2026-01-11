package game.doudizhu;

import game.common.*;
import game.doudizhu.vo.*;
import litejava.App;
import litejava.plugins.LiteJava;

import java.util.*;

/**
 * 斗地主游戏服务器
 */
public class DoudizhuServer extends GameServer<DoudizhuGame> {
    
    private final Map<String, String> roomReplays = new HashMap<>();
    private final Map<String, Integer> roomFrames = new HashMap<>();
    
    @Override
    protected String getGameName() { return "斗地主"; }
    
    @Override
    protected void startGame(RoomMgr.Room<DoudizhuGame> room) {
        DoudizhuGame game = new DoudizhuGame(room.seats.size());
        game.deal();
        room.game = game;
        room.numOfGames++;
        
        onGameStarted(room);
        
        DdzGameStartVO startVO = new DdzGameStartVO();
        startVO.numOfGames = room.numOfGames;
        startVO.bidSeat = game.currentSeat;
        broadcast(room, Cmd.GAME_START, startVO);
        
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                DdzDealVO dealVO = new DdzDealVO();
                int[] cards = game.getPlayerCards(seat.seatIndex);
                dealVO.cards = new ArrayList<>();
                for (int c : cards) dealVO.cards.add(c);
                sendTo(seat.userId, Cmd.DEAL, dealVO);
            }
        }
        app.log.info("游戏开始: 房间 " + room.id);
    }
    
    @Override
    protected void onGameStarted(RoomMgr.Room<DoudizhuGame> room) {
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                RoomSnapshot.save(seat.userId, getServerId(), room.id, 
                    getGameType(), seat.seatIndex, 3600);
            }
        }
        
        List<Long> players = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                players.add(seat.userId);
                names.add(seat.name);
            }
        }
        GameReplay.Replay replay = GameReplay.create(getGameType(), room.id, players, names);
        roomReplays.put(room.id, replay.replayId);
        roomFrames.put(room.id, 0);
        
        app.log.info("快照已保存, 录像ID: " + replay.replayId);
    }
    
    @Override
    protected void onGameEnded(RoomMgr.Room<DoudizhuGame> room, int winnerSeat, int[] scores) {
        for (int i = 0; i < room.seats.size(); i++) {
            RoomMgr.Seat seat = room.seats.get(i);
            if (seat.userId > 0) {
                boolean win = (seat.seatIndex == winnerSeat);
                int score = (scores != null && i < scores.length) ? scores[i] : 0;
                PlayerData.recordGame(seat.userId, getGameType(), win, score);
                RoomSnapshot.remove(seat.userId);
            }
        }
        
        String replayId = roomReplays.remove(room.id);
        if (replayId != null) {
            GameReplay.finish(replayId, winnerSeat);
            app.log.info("录像已保存: " + replayId);
        }
        roomFrames.remove(room.id);
    }
    
    private void recordAction(RoomMgr.Room<DoudizhuGame> room, long userId, int cmd, Object data) {
        String replayId = roomReplays.get(room.id);
        if (replayId == null) return;
        
        Integer frame = roomFrames.get(room.id);
        if (frame == null) frame = 0;
        
        GameReplay.record(replayId, frame, userId, cmd, data);
        roomFrames.put(room.id, frame + 1);
    }
    
    @Override
    protected void onGameCmd(long userId, RoomMgr.Room<DoudizhuGame> room, int cmd, Map<String, Object> data) {
        DoudizhuGame game = room.game;
        if (game == null) {
            GameException.error(ErrCode.GAME_NOT_STARTED);
        }
        
        int seatIndex = roomMgr.getUserSeat(userId);
        
        switch (cmd) {
            case DdzCmd.BID:
                doBid(room, game, seatIndex, data, userId);
                break;
            case DdzCmd.PLAY:
                doPlay(room, game, seatIndex, data, userId);
                break;
            case DdzCmd.PASS:
                doPass(room, game, seatIndex, userId);
                break;
            default:
                GameException.error(ErrCode.INVALID_ACTION);
        }
    }
    
    private void doBid(RoomMgr.Room<DoudizhuGame> room, DoudizhuGame game, int seatIndex, Map<String, Object> data, long userId) {
        if (game.status != 0) {
            GameException.error(DdzErr.NOT_BIDDING);
        }
        if (seatIndex != game.currentSeat) {
            GameException.error(ErrCode.NOT_YOUR_TURN);
        }
        
        boolean wantBid = (Boolean) data.getOrDefault("bid", false);
        
        DdzBidReqVO reqVO = new DdzBidReqVO();
        reqVO.bid = wantBid;
        recordAction(room, userId, DdzCmd.BID, reqVO);
        
        DdzBidVO bidResult = game.bid(seatIndex, wantBid);
        if (bidResult == null) {
            GameException.error(ErrCode.INVALID_ACTION);
        }
        
        DdzBidResultVO bidVO = new DdzBidResultVO();
        bidVO.seatIndex = bidResult.seat;
        bidVO.bid = bidResult.bid;
        bidVO.nextSeat = bidResult.nextBidSeat;
        bidVO.landlordSeat = bidResult.landlordSeat;
        if (bidResult.bottomCards != null) {
            bidVO.bottomCards = new ArrayList<>();
            for (int c : bidResult.bottomCards) bidVO.bottomCards.add(c);
        }
        broadcast(room, DdzCmd.BID_RESULT, bidVO);
    }
    
    private void doPlay(RoomMgr.Room<DoudizhuGame> room, DoudizhuGame game, int seatIndex, Map<String, Object> data, long userId) {
        if (game.status != 1) {
            GameException.error(DdzErr.NOT_PLAYING);
        }
        if (seatIndex != game.currentSeat) {
            GameException.error(ErrCode.NOT_YOUR_TURN);
        }
        
        List<Number> cardList = (List<Number>) data.get("cards");
        if (cardList == null || cardList.isEmpty()) {
            GameException.error(ErrCode.INVALID_CARDS);
        }
        
        int[] cards = cardList.stream().mapToInt(Number::intValue).toArray();
        
        DdzPlayReqVO reqVO = new DdzPlayReqVO();
        reqVO.cards = cards;
        recordAction(room, userId, DdzCmd.PLAY, reqVO);
        
        DdzPlayVO playResult = game.play(seatIndex, cards);
        if (playResult == null) {
            GameException.error(ErrCode.CANNOT_BEAT);
        }
        
        DdzPlayResultVO playVO = new DdzPlayResultVO();
        playVO.seatIndex = playResult.seat;
        playVO.cards = playResult.cards;
        playVO.pass = false;
        playVO.nextSeat = playResult.nextSeat;
        playVO.remainCards = playResult.remainCount;
        playVO.gameOver = playResult.gameOver;
        playVO.winner = playResult.winner;
        broadcast(room, DdzCmd.PLAY_RESULT, playVO);
        
        if (playVO.gameOver) {
            int[] scores = calculateScores(room, game, playVO.winner);
            onGameEnded(room, playVO.winner, scores);
            onGameOver(room);
        }
    }
    
    private int[] calculateScores(RoomMgr.Room<DoudizhuGame> room, DoudizhuGame game, int winner) {
        int[] scores = new int[room.seats.size()];
        int baseScore = 100;
        int mult = game.multiple > 0 ? game.multiple : 1;
        
        for (int i = 0; i < scores.length; i++) {
            if (i == winner) {
                scores[i] = baseScore * mult * 2;
            } else {
                scores[i] = -baseScore * mult;
            }
        }
        return scores;
    }
    
    private void doPass(RoomMgr.Room<DoudizhuGame> room, DoudizhuGame game, int seatIndex, long userId) {
        if (game.status != 1) {
            GameException.error(DdzErr.NOT_PLAYING);
        }
        if (seatIndex != game.currentSeat) {
            GameException.error(ErrCode.NOT_YOUR_TURN);
        }
        
        recordAction(room, userId, DdzCmd.PASS, null);
        
        DdzPlayVO passResult = game.pass(seatIndex);
        if (passResult == null) {
            GameException.error(ErrCode.CANNOT_PASS);
        }
        
        DdzPlayResultVO passVO = new DdzPlayResultVO();
        passVO.seatIndex = passResult.seat;
        passVO.pass = true;
        passVO.nextSeat = passResult.nextSeat;
        broadcast(room, DdzCmd.PLAY_RESULT, passVO);
    }
    
    @Override
    protected Object getGameState(RoomMgr.Room<DoudizhuGame> room, int seatIndex) {
        if (room.game == null) return null;
        
        DoudizhuGame game = room.game;
        DdzStateVO vo = new DdzStateVO();
        vo.status = game.status;
        vo.currentSeat = game.currentSeat;
        vo.landlordSeat = game.landlordSeat;
        int[] cards = game.getPlayerCards(seatIndex);
        vo.myCards = new ArrayList<>();
        for (int c : cards) vo.myCards.add(c);
        vo.lastCards = game.lastCards;
        vo.lastPlaySeat = game.lastPlaySeat;
        return vo;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new DoudizhuServer().start(app);
    }
}
