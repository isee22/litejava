package game.account.service;

import game.account.DB;
import game.account.entity.*;
import game.account.mapper.*;

import java.util.*;

/**
 * 签到服务 - 每日登录奖励
 */
public class SignInService {
    
    // 配置缓存
    private static List<SignInConfigEntity> configCache = null;
    private static long configCacheTime = 0;
    private static final long CACHE_TTL = 300000; // 5分钟
    
    public static class SignInResult {
        public boolean success;
        public int day;               // 签到第几天
        public int coins;             // 获得金币
        public int diamonds;          // 获得钻石
        public String msg;
    }
    
    /**
     * 获取签到状态
     */
    public static SignInRecordEntity getStatus(long userId) {
        SignInRecordEntity record = DB.execute(SignInRecordMapper.class, m -> m.findByUserId(userId));
        
        if (record == null) {
            record = new SignInRecordEntity();
            record.userId = userId;
            record.signDay = 0;
            record.lastSignTime = 0;
            
            SignInRecordEntity finalRecord = record;
            DB.execute(SignInRecordMapper.class, m -> {
                m.insert(finalRecord);
                return null;
            });
        }
        
        return record;
    }
    
    /**
     * 检查今天是否已签到
     */
    public static boolean isTodaySigned(SignInRecordEntity record) {
        if (record.lastSignTime == 0) return false;
        return isSameDay(record.lastSignTime, System.currentTimeMillis());
    }
    
    /**
     * 执行签到
     */
    public static SignInResult signIn(long userId) {
        SignInResult result = new SignInResult();
        SignInRecordEntity record = getStatus(userId);
        
        long now = System.currentTimeMillis();
        
        if (isSameDay(record.lastSignTime, now)) {
            result.success = false;
            result.msg = "今日已签到";
            return result;
        }
        
        // 如果超过一天没签到，重置签到天数
        if (record.lastSignTime > 0 && now - record.lastSignTime > 48 * 3600 * 1000L) {
            record.signDay = 0;
        }
        
        // 获取配置
        List<SignInConfigEntity> configs = getRewardConfigs();
        int maxDay = configs.size();
        if (maxDay == 0) {
            result.success = false;
            result.msg = "签到配置未设置";
            return result;
        }
        
        // 计算签到天数 (循环)
        record.signDay = (record.signDay % maxDay) + 1;
        record.lastSignTime = now;
        
        // 更新数据库
        DB.execute(SignInRecordMapper.class, m -> {
            m.update(record);
            return null;
        });
        
        // 获取奖励配置
        SignInConfigEntity config = null;
        for (SignInConfigEntity cfg : configs) {
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
            PlayerService.addCoins(userId, config.coins, "签到奖励");
        }
        if (config.diamonds > 0) {
            PlayerService.addDiamonds(userId, config.diamonds, "签到奖励");
        }
        
        // 发放道具奖励
        if (config.itemIds != null && !config.itemIds.isEmpty()) {
            String[] items = config.itemIds.split(",");
            for (String item : items) {
                String[] parts = item.split(":");
                if (parts.length == 2) {
                    int itemId = Integer.parseInt(parts[0].trim());
                    int count = Integer.parseInt(parts[1].trim());
                    ItemService.addItem(userId, itemId, count, 0);
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
    
    /**
     * 获取签到奖励配置 (带缓存)
     */
    public static List<SignInConfigEntity> getRewardConfigs() {
        long now = System.currentTimeMillis();
        if (configCache != null && now - configCacheTime < CACHE_TTL) {
            return configCache;
        }
        
        List<SignInConfigEntity> configs = DB.execute(SignInConfigMapper.class, SignInConfigMapper::findAll);
        
        // 如果数据库没有配置，使用默认配置
        if (configs == null || configs.isEmpty()) {
            configs = getDefaultConfigs();
        }
        
        configCache = configs;
        configCacheTime = now;
        return configs;
    }
    
    /**
     * 默认配置 (数据库为空时使用)
     */
    private static List<SignInConfigEntity> getDefaultConfigs() {
        List<SignInConfigEntity> list = new ArrayList<>();
        list.add(createConfig(1, 1000, 0, null, "第1天签到"));
        list.add(createConfig(2, 2000, 0, null, "第2天签到"));
        list.add(createConfig(3, 3000, 10, null, "第3天签到"));
        list.add(createConfig(4, 4000, 0, null, "第4天签到"));
        list.add(createConfig(5, 5000, 0, null, "第5天签到"));
        list.add(createConfig(6, 6000, 20, null, "第6天签到"));
        list.add(createConfig(7, 10000, 50, "1001:1", "第7天签到送双倍金币卡"));
        return list;
    }
    
    private static SignInConfigEntity createConfig(int day, int coins, int diamonds, String itemIds, String desc) {
        SignInConfigEntity cfg = new SignInConfigEntity();
        cfg.day = day;
        cfg.coins = coins;
        cfg.diamonds = diamonds;
        cfg.itemIds = itemIds;
        cfg.desc = desc;
        return cfg;
    }
    
    /**
     * 清除配置缓存 (配置更新后调用)
     */
    public static void clearConfigCache() {
        configCache = null;
    }
    
    private static boolean isSameDay(long t1, long t2) {
        if (t1 == 0) return false;
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) 
            && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}
