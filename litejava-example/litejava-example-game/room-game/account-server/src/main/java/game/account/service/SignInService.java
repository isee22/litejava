package game.account.service;

import game.account.Services;
import game.account.dao.SignInDao;
import game.account.entity.SignInConfig;
import game.account.entity.SignInRecord;
import game.account.vo.SignInResultVO;

import java.util.Calendar;
import java.util.List;

/**
 * 签到服务
 */
public class SignInService {

    private final SignInDao signInDao = new SignInDao();

    public SignInRecord getStatus(long userId) {
        SignInRecord record = signInDao.findByUserId(userId);

        if (record == null) {
            record = new SignInRecord();
            record.userId = userId;
            record.signDay = 0;
            record.lastSignTime = 0;
            signInDao.insertRecord(record);
        }

        return record;
    }

    public boolean isTodaySigned(SignInRecord record) {
        if (record.lastSignTime == 0) return false;
        return isSameDay(record.lastSignTime, System.currentTimeMillis());
    }

    public SignInResultVO signIn(long userId) {
        SignInResultVO result = new SignInResultVO();
        SignInRecord record = getStatus(userId);

        long now = System.currentTimeMillis();

        if (isSameDay(record.lastSignTime, now)) {
            result.success = false;
            result.msg = "今日已签到";
            return result;
        }

        // 超过一天没签到，重置
        if (record.lastSignTime > 0 && now - record.lastSignTime > 48 * 3600 * 1000L) {
            record.signDay = 0;
        }

        List<SignInConfig> configs = signInDao.findAllConfigs();
        int maxDay = configs != null ? configs.size() : 0;
        if (maxDay == 0) {
            result.success = false;
            result.msg = "签到配置未设置";
            return result;
        }

        record.signDay = (record.signDay % maxDay) + 1;
        record.lastSignTime = now;
        signInDao.updateRecord(record);

        SignInConfig config = null;
        for (SignInConfig cfg : configs) {
            if (cfg.day == record.signDay) {
                config = cfg;
                break;
            }
        }

        if (config == null) {
            result.success = false;
            result.msg = "奖励配置不存在";
            return result;
        }

        // 发放奖励
        if (config.coins > 0) {
            Services.player.addCoins(userId, config.coins, "签到奖励");
        }
        if (config.diamonds > 0) {
            Services.player.addDiamonds(userId, config.diamonds, "签到奖励");
        }

        // 发放道具
        if (config.itemIds != null && !config.itemIds.isEmpty()) {
            String[] items = config.itemIds.split(",");
            for (String item : items) {
                String[] parts = item.split(":");
                if (parts.length == 2) {
                    int itemId = Integer.parseInt(parts[0].trim());
                    int count = Integer.parseInt(parts[1].trim());
                    Services.item.addItem(userId, itemId, count, 0);
                }
            }
        }

        result.success = true;
        result.day = record.signDay;
        result.coins = config.coins;
        result.diamonds = config.diamonds;
        result.msg = "签到成功";

        return result;
    }

    public List<SignInConfig> getRewardConfigs() {
        return signInDao.findAllConfigs();
    }

    private boolean isSameDay(long t1, long t2) {
        if (t1 == 0) return false;
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}

