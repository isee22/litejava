package game.account.dao;

import game.account.Cache;
import game.account.DB;
import game.account.entity.Account;
import game.account.mapper.AccountMapper;

/**
 * 账号 DAO
 */
public class AccountDao {

    private static final String KEY = "account:";
    private static final String KEY_NAME = "account:name:";

    public Account findById(long id) {
        Account cached = Cache.get(KEY + id, Account.class);
        if (cached != null) return cached;

        Account account = DB.execute(AccountMapper.class, m -> m.findById(id));
        if (account != null) {
            Cache.set(KEY + id, account);
        }
        return account;
    }

    public Account findByUsername(String username) {
        String idStr = Cache.getString(KEY_NAME + username);
        if (idStr != null) {
            return findById(Long.parseLong(idStr));
        }

        Account account = DB.execute(AccountMapper.class, m -> m.findByUsername(username));
        if (account != null) {
            Cache.set(KEY + account.id, account);
            Cache.setString(KEY_NAME + username, String.valueOf(account.id));
        }
        return account;
    }

    public void insert(Account account) {
        DB.execute(AccountMapper.class, m -> {
            m.insert(account);
            return null;
        });
        Cache.set(KEY + account.id, account);
        Cache.setString(KEY_NAME + account.username, String.valueOf(account.id));
    }

    public void updateLoginTime(long id, long time) {
        DB.execute(AccountMapper.class, m -> {
            m.updateLoginTime(id, time);
            return null;
        });
        Cache.del(KEY + id);
    }
}

