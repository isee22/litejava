const { Cmd } = require('game-core');
const Doudizhu4Cmd = require('./doudizhu4-cmd');

/**
 * 四人斗地主游戏逻辑
 */
class Doudizhu4Game {
    constructor(room, broadcast, sendTo) {
        this.room = room;
        this.broadcast = broadcast;
        this.sendTo = sendTo;
    }

    /**
     * 创建两副牌
     */
    createDeck() {
        const deck = [];
        for (let d = 0; d < 2; d++) {
            for (let suit = 0; suit < 4; suit++) {
                for (let rank = 3; rank <= 15; rank++) {
                    deck.push(suit * 100 + rank);
                }
            }
            deck.push(520); // 小王
            deck.push(521); // 大王
        }
        // 洗牌
        for (let i = deck.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [deck[i], deck[j]] = [deck[j], deck[i]];
        }
        return deck;
    }

    /**
     * 开始游戏
     */
    start() {
        const deck = this.createDeck();

        // 发牌: 每人25张，底牌8张
        const hands = [[], [], [], []];
        for (let i = 0; i < 100; i++) {
            hands[i % 4].push(deck[i]);
        }
        const landlordCards = deck.slice(100);

        this.room.game = {
            hands,
            landlordCards,
            landlord: -1,
            currentBidder: 0,
            highestBid: 0,
            highestBidder: -1,
            currentTurn: -1,
            lastPlay: null,
            lastPlayer: -1,
            passCount: 0
        };

        this.broadcast(this.room, Cmd.GAME_START, null);

        // 发牌给各玩家
        this.room.seats.forEach((seat, i) => {
            if (seat.userId) {
                this.sendTo(seat.userId, Cmd.DEAL, { tiles: hands[i] });
            }
        });

        // 开始叫地主
        this.broadcast(this.room, Cmd.TURN, { seatIndex: 0, phase: 'bid' });
    }

    /**
     * 处理叫地主
     */
    onBid(seatIndex, score) {
        const game = this.room.game;
        if (!game || seatIndex !== game.currentBidder) return false;

        if (score > game.highestBid) {
            game.highestBid = score;
            game.highestBidder = seatIndex;
        }

        this.broadcast(this.room, Cmd.BID, { seatIndex, score });

        game.currentBidder = (game.currentBidder + 1) % 4;

        // 叫完一轮或有人叫3分
        if (game.currentBidder === 0 || game.highestBid === 3) {
            if (game.highestBidder >= 0) {
                this.confirmLandlord();
            } else {
                // 没人叫，重新开始
                return 'restart';
            }
        } else {
            this.broadcast(this.room, Cmd.TURN, { seatIndex: game.currentBidder, phase: 'bid' });
        }
        return true;
    }

    /**
     * 确定地主
     */
    confirmLandlord() {
        const game = this.room.game;
        game.landlord = game.highestBidder;
        game.hands[game.landlord].push(...game.landlordCards);
        game.currentTurn = game.landlord;

        this.broadcast(this.room, Cmd.TURN, {
            seatIndex: game.landlord,
            phase: 'play',
            landlord: game.landlord,
            landlordCards: game.landlordCards
        });
    }

    /**
     * 处理出牌
     */
    onPlay(seatIndex, cards) {
        const game = this.room.game;
        if (!game || seatIndex !== game.currentTurn) return false;

        // 移除手牌
        cards.forEach(c => {
            const idx = game.hands[seatIndex].indexOf(c);
            if (idx >= 0) game.hands[seatIndex].splice(idx, 1);
        });

        game.lastPlay = cards;
        game.lastPlayer = seatIndex;
        game.passCount = 0;

        this.broadcast(this.room, Cmd.PLAY, { seatIndex, cards });

        // 检查是否出完
        if (game.hands[seatIndex].length === 0) {
            const isLandlord = seatIndex === game.landlord;
            this.broadcast(this.room, Cmd.GAME_OVER, {
                winner: seatIndex,
                landlordWin: isLandlord,
                landlord: game.landlord
            });
            return 'gameover';
        }

        this.nextTurn();
        return true;
    }

    /**
     * 处理不出
     */
    onPass(seatIndex) {
        const game = this.room.game;
        if (!game || seatIndex !== game.currentTurn) return false;

        game.passCount++;
        this.broadcast(this.room, Cmd.PASS, { seatIndex });

        // 3人都不要，清空上家牌
        if (game.passCount >= 3) {
            game.lastPlay = null;
            game.lastPlayer = -1;
            game.passCount = 0;
        }

        this.nextTurn();
        return true;
    }

    /**
     * 下一个出牌
     */
    nextTurn() {
        const game = this.room.game;
        game.currentTurn = (game.currentTurn + 1) % 4;
        this.broadcast(this.room, Cmd.TURN, { seatIndex: game.currentTurn, phase: 'play' });
    }
}

module.exports = Doudizhu4Game;
