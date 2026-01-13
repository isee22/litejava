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
        
        // 开始叫地主计时
        room.startTurn(game.currentSeat, 0);
        
        onGameStarted(room);
        
        DdzGameStartVO startVO = new DdzGameStartVO();
        startVO.numOfGames = room.numOfGames;
        startVO.bidSeat = game.currentSeat;
        startVO.timeout = room.getTurnTimeout() / 1000;
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
        
        // 检查第一个玩家是否托管
        checkAndAutoPlay(room);
    }
    
    /**
     * 检查当前玩家是否托管，如果是则立即自动操作
     */
    private void checkAndAutoPlay(RoomMgr.Room<DoudizhuGame> room) {
        DoudizhuGame game = room.game;
        if (game == null) return;
        
        // 找到当前座位的玩家
        RoomMgr.Seat currentSeat = null;
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.seatIndex == room.currentSeat && seat.userId > 0) {
                currentSeat = seat;
                break;
            }
        }
        
        if (currentSeat == null) return;
        if (!currentSeat.trusteeship) return;
        
        app.log.info("托管玩家轮到，立即自动操作: userId=" + currentSeat.userId + ", 阶段=" + game.status);
        
        // 托管中，立即执行自动操作
        if (game.status == 0) {
            boolean shouldBid = (game.lastBidSeat < 0);
            autoBid(room, game, currentSeat, shouldBid);
        } else if (game.status == 1) {
            autoPlay(room, game, currentSeat);
        }
    }
    
    @Override
    protected void onTurnTimeout(RoomMgr.Room<DoudizhuGame> room, RoomMgr.Seat seat) {
        DoudizhuGame game = room.game;
        if (game == null) return;
        
        app.log.info("超时自动操作: userId=" + seat.userId + ", 阶段=" + game.status);
        
        if (game.status == 0) {
            // 叫地主阶段
            // 如果还没人叫地主，第一个超时的自动叫地主；否则自动不抢
            boolean shouldBid = (game.lastBidSeat < 0);
            autoBid(room, game, seat, shouldBid);
        } else if (game.status == 1) {
            // 出牌阶段：自动出牌或不出
            autoPlay(room, game, seat);
        }
    }
    
    /**
     * 自动叫地主/不叫
     */
    private void autoBid(RoomMgr.Room<DoudizhuGame> room, DoudizhuGame game, RoomMgr.Seat seat, boolean wantBid) {
        DdzBidVO bidResult = game.bid(seat.seatIndex, wantBid);
        if (bidResult == null) return;
        
        recordAction(room, seat.userId, DdzCmd.BID, Map.of("bid", wantBid, "auto", true));
        
        DdzBidResultVO bidVO = new DdzBidResultVO();
        bidVO.seatIndex = bidResult.seat;
        bidVO.bid = wantBid;
        bidVO.nextSeat = bidResult.nextBidSeat;
        bidVO.landlordSeat = bidResult.landlordSeat;
        bidVO.redeal = bidResult.redeal;
        if (bidResult.bottomCards != null) {
            bidVO.bottomCards = new ArrayList<>();
            for (int c : bidResult.bottomCards) bidVO.bottomCards.add(c);
        }
        broadcast(room, DdzCmd.BID_RESULT, bidVO);
        
        // 更新下一个玩家的计时
        if (bidResult.landlordSeat >= 0) {
            // 确定地主，开始出牌阶段
            room.startTurn(game.currentSeat, 1);
            checkAndAutoPlay(room);
        } else if (bidResult.redeal) {
            // 重新发牌，需要重新发送手牌给所有玩家
            for (RoomMgr.Seat s : room.seats) {
                if (s.userId > 0) {
                    DdzDealVO dealVO = new DdzDealVO();
                    int[] cards = game.getPlayerCards(s.seatIndex);
                    dealVO.cards = new ArrayList<>();
                    for (int c : cards) dealVO.cards.add(c);
                    sendTo(s.userId, Cmd.DEAL, dealVO);
                }
            }
            room.startTurn(game.currentSeat, 0);
            checkAndAutoPlay(room);
        } else {
            // 继续叫地主
            room.startTurn(bidResult.nextBidSeat, 0);
            checkAndAutoPlay(room);
        }
    }
    
    /**
     * 自动不叫 (兼容旧代码)
     */
    private void autoNoBid(RoomMgr.Room<DoudizhuGame> room, DoudizhuGame game, RoomMgr.Seat seat) {
        autoBid(room, game, seat, false);
    }
    
    /**
     * 自动出牌或不出
     */
    private void autoPlay(RoomMgr.Room<DoudizhuGame> room, DoudizhuGame game, RoomMgr.Seat seat) {
        // 如果可以不出，就不出
        if (game.lastCards.length > 0 && game.lastPlaySeat != seat.seatIndex) {
            autoPass(room, game, seat);
            return;
        }
        
        // 必须出牌，找最小的单张
        int[] myCards = game.getPlayerCards(seat.seatIndex);
        if (myCards.length == 0) return;
        
        // 出最小的一张牌
        int minCard = myCards[myCards.length - 1];
        int[] cards = new int[]{minCard};
        
        DdzPlayVO playResult = game.play(seat.seatIndex, cards);
        if (playResult == null) {
            // 单张不行，尝试其他牌型（简单处理：直接不出）
            if (game.lastCards.length > 0) {
                autoPass(room, game, seat);
            }
            return;
        }
        
        recordAction(room, seat.userId, DdzCmd.PLAY, Map.of("cards", cards, "auto", true));
        
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
            DdzGameOverVO overVO = new DdzGameOverVO();
            overVO.winner = playVO.winner;
            overVO.landlordWin = (playVO.winner == game.landlordSeat);
            overVO.scores = scores;
            broadcast(room, Cmd.GAME_OVER, overVO);
            onGameEnded(room, playVO.winner, scores);
            onGameOver(room);
        } else {
            room.startTurn(playResult.nextSeat, 1);
            checkAndAutoPlay(room);
        }
    }
    
    /**
     * 自动不出
     */
    private void autoPass(RoomMgr.Room<DoudizhuGame> room, DoudizhuGame game, RoomMgr.Seat seat) {
        DdzPlayVO passResult = game.pass(seat.seatIndex);
        if (passResult == null) return;
        
        recordAction(room, seat.userId, DdzCmd.PASS, Map.of("auto", true));
        
        DdzPlayResultVO passVO = new DdzPlayResultVO();
        passVO.seatIndex = passResult.seat;
        passVO.pass = true;
        passVO.nextSeat = passResult.nextSeat;
        passVO.clearLast = passResult.clearLast;
        passVO.gameOver = false;
        passVO.winner = -1;
        broadcast(room, DdzCmd.PLAY_RESULT, passVO);
        
        room.startTurn(passResult.nextSeat, 1);
        checkAndAutoPlay(room);
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
        // 调用父类默认实现，提交结算到 AccountServer
        super.onGameEnded(room, winnerSeat, scores);
        
        // 清理快照
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                RoomSnapshot.remove(seat.userId);
            }
        }
        
        // 完成录像
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
        bidVO.redeal = bidResult.redeal;
        if (bidResult.bottomCards != null) {
            bidVO.bottomCards = new ArrayList<>();
            for (int c : bidResult.bottomCards) bidVO.bottomCards.add(c);
        }
        broadcast(room, DdzCmd.BID_RESULT, bidVO);
        
        // 更新下一个玩家的计时
        if (bidResult.landlordSeat >= 0) {
            room.startTurn(game.currentSeat, 1);
            checkAndAutoPlay(room);
        } else if (bidResult.redeal) {
            room.startTurn(game.currentSeat, 0);
            checkAndAutoPlay(room);
        } else {
            room.startTurn(bidResult.nextBidSeat, 0);
            checkAndAutoPlay(room);
        }
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
            
            // 广播游戏结束
            DdzGameOverVO overVO = new DdzGameOverVO();
            overVO.winner = playVO.winner;
            overVO.landlordWin = (playVO.winner == game.landlordSeat);
            overVO.scores = scores;
            broadcast(room, Cmd.GAME_OVER, overVO);
            
            onGameEnded(room, playVO.winner, scores);
            onGameOver(room);
        } else {
            // 更新下一个玩家的计时
            room.startTurn(playResult.nextSeat, 1);
            checkAndAutoPlay(room);
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
        passVO.clearLast = passResult.clearLast;
        passVO.gameOver = false;
        passVO.winner = -1;
        broadcast(room, DdzCmd.PLAY_RESULT, passVO);
        
        // 更新下一个玩家的计时
        room.startTurn(passResult.nextSeat, 1);
        checkAndAutoPlay(room);
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
        vo.bottomCards = game.bottomCards;
        return vo;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new DoudizhuServer().start(app);
    }
}
