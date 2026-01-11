package game.account;

import game.account.controller.*;
import game.account.mapper.*;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.database.MyBatisPlugin;

import java.util.*;

/**
 * 账号服务器 (Account Server)
 * 
 * BabyKylin 模式 - 独立服务，无外部依赖：
 * - 登录/注册
 * - 玩家数据
 * - 排行榜、商城、好友、礼物
 * 
 * 房间/匹配功能在 HallServer
 */
public class AccountServer {
    
    private App app;
    private int httpPort;
    
    public void start(App app) {
        this.app = app;
        httpPort = app.conf.getInt("server", "httpPort", 8101);
        
        // 使用内存缓存 (生产环境可换 Redis)
        app.log.info("使用内存缓存");
        
        // MyBatis
        app.use(new MyBatisPlugin(
            AccountMapper.class,
            PlayerMapper.class, 
            GameConfigMapper.class,
            SignInConfigMapper.class,
            SignInRecordMapper.class,
            ItemConfigMapper.class,
            PlayerItemMapper.class,
            FriendMapper.class,
            FriendRequestMapper.class
        ));
        
        // 注册路由
        registerRoutes();
        
        app.log.info("=== Account Server ===");
        app.log.info("HTTP: http://localhost:" + httpPort);
        app.run(httpPort);
    }
    
    private void registerRoutes() {
        // 账号相关
        AuthController.register(app);
        PlayerController.register(app);
        
        // 游客/用户信息 (迁移自 account_server.js, dealer_api.js)
        GuestController.register(app);
        ServerController.register(app);
        
        // 社交/商城
        ShopController.register(app);
        GiftController.register(app);
        RankController.register(app);
        FriendController.register(app);
        ChatController.register(app);
        
        // 配置
        ConfigController.register(app);
        
        // 健康检查
        app.get("/health", ctx -> ctx.ok(Map.of("status", "UP")));
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new AccountServer().start(app);
    }
}
