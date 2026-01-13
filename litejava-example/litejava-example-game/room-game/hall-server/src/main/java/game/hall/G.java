package game.hall;

import game.hall.service.MatchService;
import game.hall.service.RoomService;
import game.hall.service.ServerRegistry;
import litejava.App;
import litejava.plugins.cache.RedisCachePlugin;

/**
 * 全局单例容器
 */
public class G {
    
    public static App app;
    public static RedisCachePlugin cache;
    
    // Services
    public static ServerRegistry registry;
    public static RoomService roomService;
    public static MatchService matchService;
    
    /**
     * 初始化所有单例
     */
    public static void init(App app, RedisCachePlugin cache) {
        G.app = app;
        G.cache = cache;
        
        G.registry = new ServerRegistry();
        G.roomService = new RoomService();
        G.matchService = new MatchService();
    }
}
