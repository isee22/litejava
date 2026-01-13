package game.account;

import game.account.controller.*;
import game.account.mapper.*;
import game.account.service.ReplayService;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.cache.CachePlugin;
import litejava.plugins.cache.MemoryCachePlugin;
import litejava.plugins.cache.RedisCachePlugin;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.http.RecoveryPlugin;

import java.util.*;

/**
 * 账号服务器 (Account Server)
 * 
 * 支持两种模式:
 * - 单机模式: 使用 MemoryCachePlugin
 * - 集群模式: 使用 RedisCachePlugin，配置同步到 Redis 供 HallServer 读取
 */
public class AccountServer {
    
    private App app;
    private int httpPort;
    private CachePlugin cache;
    
    public void start(App app) {
        this.app = app;
        httpPort = app.conf.getInt("server", "httpPort", 8101);

        // 全局异常处理
        app.use(new RecoveryPlugin());

        // 初始化缓存
        cache = createCache();
        app.use(cache);
        Cache.instance = cache;

        // 初始化服务
        Services.init();

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
            FriendRequestMapper.class,
            GameReplayMapper.class
        ));

        // 注册路由
        registerRoutes();

        // 服务启动后同步游戏配置到缓存 (供 HallServer 读取)
        app.onReady(this::syncGameConfigToCache);

        String mode = cache instanceof RedisCachePlugin ? "集群模式 (Redis)" : "单机模式 (Memory)";
        app.log.info("=== Account Server ===");
        app.log.info("Mode: " + mode);
        app.log.info("HTTP: http://localhost:" + httpPort);
        app.run(httpPort);
    }
    
    /**
     * 根据配置创建缓存插件
     */
    private CachePlugin createCache() {
        String redisHost = app.conf.getString("redis", "host", null);
        
        if (redisHost != null && !redisHost.isEmpty()) {
            app.log.info("Using Redis cache: " + redisHost);
            return new RedisCachePlugin();
        }
        
        app.log.info("Using Memory cache (single instance mode)");
        return new MemoryCachePlugin();
    }
    
    /**
     * 同步游戏配置到缓存
     */
    private void syncGameConfigToCache() {
        Services.config.syncAll();
        app.log.info("Game configs synced to cache");
    }
    
    private void registerRoutes() {
        // 账号相关
        AuthController.register(app);
        PlayerController.register(app);
        
        // 游客/用户信息
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
        
        // 录像
        GameReplayMapper replayMapper = app.getPlugin(MyBatisPlugin.class).getMapper(GameReplayMapper.class);
        ReplayService replayService = new ReplayService(replayMapper);
        new ReplayController(replayService).register(app);
        
        // 健康检查
        app.get("/health", ctx -> ctx.ok(Map.of("status", "UP")));
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new AccountServer().start(app);
    }
}

