package game.account.controller;

import game.account.entity.AccountEntity;
import game.account.entity.PlayerEntity;
import game.account.service.AuthService;
import game.account.service.PlayerService;
import game.account.vo.GuestLoginRespVO;
import game.account.vo.BaseInfoVO;
import game.account.vo.UserInfoVO;
import game.account.vo.AddGemsReqVO;
import litejava.App;

import java.security.MessageDigest;

/**
 * 游客/用户信息控制器
 * 
 * 迁移自 account_server.js: /guest, /base_info
 * 迁移自 dealer_api.js: /get_user_info, /add_user_gems
 */
public class GuestController {
    
    private static String accountPriKey = "";
    private static String hallAddr = "";
    
    public static void register(App app) {
        accountPriKey = app.conf.getString("app", "priKey", "game_secret_key");
        String hallIp = app.conf.getString("hall", "ip", "localhost");
        int hallPort = app.conf.getInt("hall", "port", 7100);
        hallAddr = hallIp + ":" + hallPort;
        
        // 游客登录
        app.get("/guest", ctx -> {
            String deviceId = ctx.queryParam("account");
            // IP 用于签名，从 X-Forwarded-For 或使用默认值
            String ip = ctx.header("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = "127.0.0.1";
            }
            
            String account = "guest_" + deviceId;
            String sign = md5(account + ip + accountPriKey);
            
            // 确保游客账号存在
            AuthService.getOrCreateGuest(account);
            
            GuestLoginRespVO vo = new GuestLoginRespVO();
            vo.account = account;
            vo.halladdr = hallAddr;
            vo.sign = sign;
            ctx.ok(vo);
        });
        
        // 获取用户基本信息
        app.get("/base_info", ctx -> {
            long userId = Long.parseLong(ctx.queryParam("userid"));
            PlayerEntity player = PlayerService.get(userId);
            
            BaseInfoVO vo = new BaseInfoVO();
            if (player != null) {
                vo.name = player.name;
                vo.sex = player.sex;
                vo.headimgurl = player.avatar;
            }
            ctx.ok(vo);
        });
        
        // === dealer_api 接口 ===
        
        // 获取用户信息
        app.get("/get_user_info", ctx -> {
            long userId = Long.parseLong(ctx.queryParam("userid"));
            PlayerEntity player = PlayerService.get(userId);
            
            if (player == null) {
                ctx.fail(1, "用户不存在");
                return;
            }
            
            UserInfoVO vo = new UserInfoVO();
            vo.userid = userId;
            vo.name = player.name;
            vo.gems = player.diamonds;
            vo.headimg = player.avatar;
            ctx.ok(vo);
        });
        
        // 添加钻石
        app.get("/add_user_gems", ctx -> {
            long userId = Long.parseLong(ctx.queryParam("userid"));
            int gems = Integer.parseInt(ctx.queryParam("gems"));
            
            boolean success = PlayerService.addDiamonds(userId, gems, "dealer_api");
            if (success) {
                ctx.ok("ok");
            } else {
                ctx.fail(1, "failed");
            }
        });
    }
    
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
