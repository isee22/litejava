package game.account.service;

import game.account.entity.GameReplayEntity;
import game.account.mapper.GameReplayMapper;
import game.account.vo.ReplaySummaryVO;

import java.util.*;

/**
 * 录像服务
 */
public class ReplayService {
    
    private final GameReplayMapper mapper;
    
    public ReplayService(GameReplayMapper mapper) {
        this.mapper = mapper;
    }
    
    /**
     * 创建录像
     */
    public void create(String replayId, String gameType, String roomId, 
                       List<Long> players, List<String> names, String initState) {
        GameReplayEntity entity = new GameReplayEntity();
        entity.replayId = replayId;
        entity.gameType = gameType;
        entity.roomId = roomId;
        entity.players = joinLongs(players);
        entity.playerNames = String.join(",", names);
        entity.winner = -1;
        entity.initState = initState;
        entity.actions = "[]";
        entity.actionCount = 0;
        entity.startTime = System.currentTimeMillis();
        entity.endTime = 0;
        
        mapper.insert(entity);
        
        // 建立用户索引
        long now = System.currentTimeMillis();
        for (Long userId : players) {
            mapper.insertUserReplay(userId, replayId, now);
        }
    }
    
    /**
     * 完成录像
     */
    public void finish(String replayId, int winner, String actionsJson, int actionCount) {
        GameReplayEntity entity = new GameReplayEntity();
        entity.replayId = replayId;
        entity.winner = winner;
        entity.actions = actionsJson;
        entity.actionCount = actionCount;
        entity.endTime = System.currentTimeMillis();
        mapper.update(entity);
    }
    
    /**
     * 获取录像详情
     */
    public GameReplayEntity get(String replayId) {
        return mapper.findById(replayId);
    }
    
    /**
     * 获取用户录像列表
     */
    public List<ReplaySummaryVO> getUserReplays(long userId, int limit) {
        List<GameReplayEntity> list = mapper.findByUserId(userId, limit);
        List<ReplaySummaryVO> result = new ArrayList<>();
        for (GameReplayEntity e : list) {
            result.add(ReplaySummaryVO.from(e));
        }
        return result;
    }
    
    /**
     * 获取录像总数
     */
    public int count() {
        return mapper.count();
    }
    
    /**
     * 清理旧录像 (保留最近 N 天)
     */
    public int cleanOldReplays(int days) {
        long beforeTime = System.currentTimeMillis() - days * 24L * 3600 * 1000;
        int count1 = mapper.deleteOldUserReplays(beforeTime);
        int count2 = mapper.deleteOldReplays(beforeTime);
        return count2;
    }
    
    private String joinLongs(List<Long> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
