package game.account.dao;

import game.account.Cache;
import game.account.DB;
import game.account.entity.SignInConfig;
import game.account.entity.SignInRecord;
import game.account.mapper.SignInConfigMapper;
import game.account.mapper.SignInRecordMapper;

import java.util.List;

/**
 * 签到 DAO
 */
public class SignInDao {

    private static final String KEY_CONFIG = "signin:config";
    private static final String KEY_RECORD = "signin:record:";

    // ========== 签到配置 ==========

    @SuppressWarnings("unchecked")
    public List<SignInConfig> findAllConfigs() {
        List<SignInConfig> cached = Cache.get(KEY_CONFIG, List.class);
        if (cached != null) return cached;

        List<SignInConfig> list = DB.execute(SignInConfigMapper.class, SignInConfigMapper::findAll);
        if (list != null) {
            Cache.set(KEY_CONFIG, list);
        }
        return list;
    }

    public void evictConfigCache() {
        Cache.del(KEY_CONFIG);
    }

    // ========== 签到记录 ==========

    public SignInRecord findByUserId(long userId) {
        SignInRecord cached = Cache.get(KEY_RECORD + userId, SignInRecord.class);
        if (cached != null) return cached;

        SignInRecord record = DB.execute(SignInRecordMapper.class, m -> m.findByUserId(userId));
        if (record != null) {
            Cache.set(KEY_RECORD + userId, record);
        }
        return record;
    }

    public void insertRecord(SignInRecord record) {
        DB.execute(SignInRecordMapper.class, m -> {
            m.insert(record);
            return null;
        });
        Cache.set(KEY_RECORD + record.userId, record);
    }

    public void updateRecord(SignInRecord record) {
        DB.execute(SignInRecordMapper.class, m -> {
            m.update(record);
            return null;
        });
        Cache.set(KEY_RECORD + record.userId, record);
    }
}

