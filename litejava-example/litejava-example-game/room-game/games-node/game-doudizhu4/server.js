const fs = require('fs');
const yaml = require('js-yaml');
const { GameServer, Cmd } = require('game-core');
const Doudizhu4Cmd = require('./src/doudizhu4-cmd');
const Doudizhu4Game = require('./src/game-logic');

/**
 * 四人斗地主游戏服务器
 */
class Doudizhu4Server extends GameServer {
    getGameName() {
        return '四人斗地主 (Node.js)';
    }

    startGame(room) {
        const game = new Doudizhu4Game(
            room,
            this.broadcast.bind(this),
            this.sendTo.bind(this)
        );
        game.start();
        console.log(`游戏开始: 房间 ${room.id}`);
    }

    onGameCmd(userId, room, cmd, data) {
        if (!room.game) return;

        const seatIndex = this.roomManager.getSeatIndex(room, userId);
        const game = new Doudizhu4Game(
            room,
            this.broadcast.bind(this),
            this.sendTo.bind(this)
        );

        switch (cmd) {
            case Doudizhu4Cmd.BID: {
                const result = game.onBid(seatIndex, data.score);
                if (result === 'restart') {
                    this.roomManager.resetRoom(room);
                    this.startGame(room);
                }
                break;
            }
            case Doudizhu4Cmd.PLAY: {
                const result = game.onPlay(seatIndex, data.cards);
                if (result === 'gameover') {
                    this.roomManager.resetRoom(room);
                }
                break;
            }
            case Doudizhu4Cmd.PASS:
                game.onPass(seatIndex);
                break;
        }
    }
}

// 读取配置并启动
const config = yaml.load(fs.readFileSync('config.yml', 'utf8'));
new Doudizhu4Server(config).start();
