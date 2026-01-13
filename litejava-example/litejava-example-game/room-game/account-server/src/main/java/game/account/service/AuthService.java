package game.account.service;

import game.common.GameException;
import game.account.Services;
import game.account.dao.AccountDao;
import game.account.entity.Account;

import java.security.MessageDigest;

/**
 * 认证服务
 */
public class AuthService {

    private final AccountDao accountDao = new AccountDao();

    public Account register(String username, String password) {
        if (username == null || username.length() < 2 || username.length() > 16) {
            GameException.error(1, "用户名长度需要2-16个字符");
        }
        if (password == null || password.length() < 4) {
            GameException.error(2, "密码长度至少4个字符");
        }

        Account existing = accountDao.findByUsername(username);
        if (existing != null) {
            GameException.error(3, "用户名已存在");
        }

        long now = System.currentTimeMillis();
        Account account = new Account();
        account.username = username;
        account.password = hashPassword(password);
        account.createTime = now;
        account.lastLoginTime = now;

        accountDao.insert(account);
        Services.player.getOrCreate(account.id, username);

        return account;
    }

    public Account login(String username, String password) {
        if (username == null || username.isBlank()) {
            GameException.error(1, "请输入用户名");
        }
        if (password == null || password.isBlank()) {
            GameException.error(2, "请输入密码");
        }

        Account account = accountDao.findByUsername(username);
        if (account == null) {
            GameException.error(3, "用户名或密码错误");
        }

        if (!account.password.equals(hashPassword(password))) {
            GameException.error(3, "用户名或密码错误");
        }

        long now = System.currentTimeMillis();
        accountDao.updateLoginTime(account.id, now);
        account.lastLoginTime = now;

        return account;
    }

    public Account getOrCreateGuest(String guestAccount) {
        Account existing = accountDao.findByUsername(guestAccount);
        if (existing != null) {
            return existing;
        }

        long now = System.currentTimeMillis();
        Account account = new Account();
        account.username = guestAccount;
        account.password = "";
        account.createTime = now;
        account.lastLoginTime = now;

        accountDao.insert(account);
        Services.player.getOrCreate(account.id, guestAccount);

        return account;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }
}

