package game.hall;

import game.hall.service.ClientService;
import game.hall.service.MatchService;
import game.hall.service.RoomService;
import game.hall.util.HttpUtil;
import game.hall.util.SignUtil;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.cache.MemoryCachePlugin;

import java.util.concurrent.*;

/**
 * 大厅服务器 (Hall Server) - BabyKylin 模式
 * 
 * 对应原作:
 * - app.js (入口)
 * - room_service.js -> RoomService.java
 * - client_service.js -> ClientService.java
 */
public class HallServer {
    
    private App app;
    private RoomService roomService;
    private ClientService clientService;
    private MatchService matchService;
    
    public void start(App app) {
        this.app = app;
        int port = app.conf.getInt("server", "httpPort", 8201);
        String roomPriKey = app.conf.getString("server", "roomPriKey", "ROOM_PRI_KEY_2024");
        String accountPriKey = app.conf.getString("server", "accountPriKey", "ACCOUNT_PRI_KEY_2024");
        
        // 初始化组件
        MemoryCachePlugin cache = new MemoryCachePlugin();
        app.use(cache);
        
        SignUtil sign = new SignUtil(roomPriKey);
        HttpUtil http = new HttpUtil(app);
        
        // 初始化服务
        roomService = new RoomService(app, cache, sign, http);
        clientService = new ClientService(app, cache, sign, roomService, accountPriKey);
        matchService = new MatchService(app, cache, sign, roomService, clientService);
        
        // 注册路由 (各 Service 自己注册)
        roomService.setupRoutes();
        clientService.setupRoutes();
        matchService.setupRoutes();
        
        // 管理路由
        roomService.setupAdminRoutes();
        clientService.setupAdminRoutes();
        matchService.setupAdminRoutes();
        
        // 启动定时任务
        startScheduler();
        
        app.log.info("=== Hall Server (BabyKylin Mode) ===");
        app.log.info("HTTP: http://localhost:" + port);
        app.run(port);
    }
    
    private void startScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(matchService::processMatch, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(roomService::cleanup, 30, 30, TimeUnit.SECONDS);
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new HallServer().start(app);
    }
}
