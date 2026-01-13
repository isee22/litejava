package game.account.service;

import game.account.dao.PlayerDao;
import game.account.entity.Player;
import game.account.vo.RankVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 排行榜服务
 */
public class RankService {

    private final PlayerDao playerDao = new PlayerDao();

    public List<RankVO> getCoinRank(int limit) {
        List<Player> players = playerDao.findAll();
        if (players == null) return new ArrayList<>();

        players.sort(Comparator.comparingLong(p -> -p.coins));

        List<RankVO> result = new ArrayList<>();
        int rank = 1;
        for (Player p : players) {
            if (rank > limit) break;

            RankVO vo = new RankVO();
            vo.rank = rank++;
            vo.userId = p.userId;
            vo.name = p.name;
            vo.avatar = p.avatar;
            vo.level = p.level;
            vo.value = p.coins;
            result.add(vo);
        }
        return result;
    }

    public List<RankVO> getWinRank(int limit) {
        List<Player> players = playerDao.findAll();
        if (players == null) return new ArrayList<>();

        players.sort(Comparator.comparingInt(p -> -p.winGames));

        List<RankVO> result = new ArrayList<>();
        int rank = 1;
        for (Player p : players) {
            if (rank > limit) break;

            RankVO vo = new RankVO();
            vo.rank = rank++;
            vo.userId = p.userId;
            vo.name = p.name;
            vo.avatar = p.avatar;
            vo.level = p.level;
            vo.value = p.winGames;
            result.add(vo);
        }
        return result;
    }
}

