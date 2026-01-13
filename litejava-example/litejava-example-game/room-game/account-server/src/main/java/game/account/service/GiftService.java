package game.account.service;

import game.common.GameException;
import game.account.Services;
import game.account.entity.ItemConfig;
import game.account.entity.Player;
import game.account.entity.PlayerItem;
import game.account.vo.CharmRankVO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 礼物服务 - 玩家间送礼
 */
public class GiftService {

    public static class GiftRecord {
        public long id;
        public long fromUserId;
        public long toUserId;
        public int giftId;
        public String giftName;
        public int charm;
        public long createTime;
    }

    // 玩家魅力值 (生产环境应存数据库)
    private final Map<Long, Integer> charmValues = new ConcurrentHashMap<>();

    // 礼物记录 (生产环境应存数据库)
    private final List<GiftRecord> giftRecords = Collections.synchronizedList(new ArrayList<>());
    private long recordIdSeq = 1;

    public void sendGift(long fromUserId, long toUserId, int giftId, int count) {
        if (fromUserId == toUserId) {
            GameException.error(1, "不能给自己送礼");
        }

        // 检查背包是否有该礼物
        List<PlayerItem> items = Services.item.getPlayerItems(fromUserId);
        PlayerItem giftItem = null;
        for (PlayerItem item : items) {
            if (item.itemId == giftId && item.count >= count) {
                giftItem = item;
                break;
            }
        }

        // 如果背包没有，尝试直接购买
        if (giftItem == null) {
            Services.item.buyWithDiamond(fromUserId, giftId, count);
        } else {
            // 使用道具
            for (int i = 0; i < count; i++) {
                Services.item.useItem(fromUserId, giftId);
            }
        }

        // 获取礼物配置
        ItemConfig giftCfg = Services.item.getConfig(giftId);
        if (giftCfg == null || !"gift".equals(giftCfg.type)) {
            GameException.error(2, "不是有效的礼物");
        }

        // 解析效果获取魅力值
        int charm = parseCharm(giftCfg.effect) * count;

        if (charm > 0) {
            charmValues.merge(toUserId, charm, Integer::sum);
        }

        // 记录
        for (int i = 0; i < count; i++) {
            GiftRecord record = new GiftRecord();
            record.id = recordIdSeq++;
            record.fromUserId = fromUserId;
            record.toUserId = toUserId;
            record.giftId = giftId;
            record.giftName = giftCfg.name;
            record.charm = charm / count;
            record.createTime = System.currentTimeMillis();
            giftRecords.add(record);
        }

        // 只保留最近1000条记录
        while (giftRecords.size() > 1000) {
            giftRecords.remove(0);
        }
    }

    /**
     * 解析效果字符串中的魅力值
     * 格式: {"charm":10}
     */
    private int parseCharm(String effect) {
        if (effect == null || effect.isEmpty()) return 0;
        try {
            int idx = effect.indexOf("\"charm\":");
            if (idx < 0) return 0;
            int start = idx + 8;
            int end = start;
            while (end < effect.length() && (Character.isDigit(effect.charAt(end)) || effect.charAt(end) == '-')) {
                end++;
            }
            return Integer.parseInt(effect.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    public int getCharm(long userId) {
        return charmValues.getOrDefault(userId, 0);
    }

    public List<GiftRecord> getReceivedGifts(long userId, int limit) {
        List<GiftRecord> result = new ArrayList<>();
        for (int i = giftRecords.size() - 1; i >= 0 && result.size() < limit; i--) {
            GiftRecord r = giftRecords.get(i);
            if (r.toUserId == userId) {
                result.add(r);
            }
        }
        return result;
    }

    public List<GiftRecord> getSentGifts(long userId, int limit) {
        List<GiftRecord> result = new ArrayList<>();
        for (int i = giftRecords.size() - 1; i >= 0 && result.size() < limit; i--) {
            GiftRecord r = giftRecords.get(i);
            if (r.fromUserId == userId) {
                result.add(r);
            }
        }
        return result;
    }

    public List<CharmRankVO> getCharmRank(int limit) {
        List<Map.Entry<Long, Integer>> sorted = new ArrayList<>(charmValues.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        List<CharmRankVO> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
            Map.Entry<Long, Integer> entry = sorted.get(i);
            Player player = Services.player.get(entry.getKey());

            CharmRankVO item = new CharmRankVO();
            item.rank = i + 1;
            item.userId = entry.getKey();
            item.name = player != null ? player.name : "玩家" + entry.getKey();
            item.charm = entry.getValue();
            result.add(item);
        }
        return result;
    }
}

