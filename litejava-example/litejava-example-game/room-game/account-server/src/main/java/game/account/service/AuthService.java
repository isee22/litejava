package game.account.service;

import game.account.AccountException;
import game.account.DB;
import game.account.entity.AccountEntity;
import game.account.entity.PlayerEntity;
import game.account.mapper.AccountMapper;

import java.security.MessageDigest;

/**
 * 认证服务
 */
public class AuthService {
    
    /**
     * 注册
     */
    public static AccountEntity register(String username, String password) {
        if (username == null || username.length() < 2 || username.length() > 16) {
            AccountException.error(1, "用户名长度需要2-16个字符");
        }
        if (password == null || password.length() < 4) {
            AccountException.error(2, "密码长度至少4个字符");
        }
        
        // 检查用户名是否已存在
        AccountEntity existing = DB.execute(AccountMapper.class, m -> m.findByUsername(username));
        if (existing != null) {
            AccountException.error(3, "用户名已存在");
        }
        
        long now = System.currentTimeMillis();
        AccountEntity account = new AccountEntity();
        account.username = username;
        account.password = hashPassword(password);
        account.createTime = now;
        account.lastLoginTime = now;
        
        DB.execute(AccountMapper.class, m -> m.insert(account));
        
        // 创建玩家数据
        PlayerService.getOrCreate(account.id, username);
        
        return account;
    }
    
    /**
     * 登录
     */
    public static AccountEntity login(String username, String password) {
        if (username == null || username.isBlank()) {
            AccountException.error(1, "请输入用户名");
        }
        if (password == null || password.isBlank()) {
            AccountException.error(2, "请输入密码");
        }
        
        AccountEntity account = DB.execute(AccountMapper.class, m -> m.findByUsername(username));
        if (account == null) {
            AccountException.error(3, "用户名或密码错误");
        }
        
        if (!account.password.equals(hashPassword(password))) {
            AccountException.error(3, "用户名或密码错误");
        }
        
        // 更新登录时间
        long now = System.currentTimeMillis();
        DB.execute(AccountMapper.class, m -> m.updateLoginTime(account.id, now));
        account.lastLoginTime = now;
        
        return account;
    }
    
    /**
     * 获取或创建游客账号
     */
    public static AccountEntity getOrCreateGuest(String guestAccount) {
        AccountEntity existing = DB.execute(AccountMapper.class, m -> m.findByUsername(guestAccount));
        if (existing != null) {
            return existing;
        }
        
        long now = System.currentTimeMillis();
        AccountEntity account = new AccountEntity();
        account.username = guestAccount;
        account.password = "";  // 游客无密码
        account.createTime = now;
        account.lastLoginTime = now;
        
        DB.execute(AccountMapper.class, m -> m.insert(account));
        
        // 创建玩家数据
        PlayerService.getOrCreate(account.id, guestAccount);
        
        return account;
    }
    
    /**
     * 密码哈希 (简单实现，生产环境建议用 BCrypt)
     */
    private static String hashPassword(String password) {
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
