package game.hall.service;

import game.hall.G;
import game.hall.cache.CacheKeys;
import game.hall.model.ServerInfo;
import litejava.App;
import litejava.plugins.cache.RedisCachePlugin;

import java.util.*;

/**
 * GameServer 注册中心
 */
public class ServerRegistry {
    
    public App app = G.app;
    public RedisCachePlugin cache = G.cache;
    
    /**
     * 注册 GameServer
     */
    public ServerInfo register(String clientip, int clientport, int httpPort, int load, String gameType, String realIp) {
        String id = clientip + ":" + clientport;
        String ip = (realIp != null && !realIp.isEmpty()) ? realIp : clientip;
        
        ServerInfo existing = get(id);
        if (existing != null) {
            existing.load = load;
            existing.lastHeartbeat = System.currentTimeMillis();
            save(existing);
            return existing;
        }
        
        ServerInfo info = new ServerInfo();
        info.id = id;
        info.ip = ip;
        info.clientip = clientip;
        info.clientport = clientport;
        info.httpPort = httpPort;
        info.load = load;
        info.gameType = gameType;
        info.lastHeartbeat = System.currentTimeMillis();
        
        save(info);
        cache.sadd(CacheKeys.SERVER_LIST, id);
        
        app.log.info("GameServer 注册: " + id + " (" + gameType + ")");
        return info;
    }
    
    public void heartbeat(String serverId, int load) {
        ServerInfo info = get(serverId);
        if (info != null) {
            info.load = load;
            info.lastHeartbeat = System.currentTimeMillis();
            save(info);
        }
    }
    
    public ServerInfo choose(String gameType) {
        ServerInfo best = null;
        long now = System.currentTimeMillis();
        
        for (ServerInfo info : getAll()) {
            if (now - info.lastHeartbeat > 60000) continue;
            if (gameType != null && !gameType.equals(info.gameType)) continue;
            if (best == null || info.load < best.load) {
                best = info;
            }
        }
        return best;
    }
    
    public ServerInfo get(String serverId) {
        String json = cache.get(CacheKeys.server(serverId));
        if (json == null) return null;
        return app.json.parse(json, ServerInfo.class);
    }
    
    public List<ServerInfo> getAll() {
        List<ServerInfo> servers = new ArrayList<>();
        Set<String> ids = cache.smembers(CacheKeys.SERVER_LIST);
        if (ids == null) return servers;
        
        for (String id : ids) {
            ServerInfo info = get(id);
            if (info != null) servers.add(info);
        }
        return servers;
    }
    
    public void cleanup() {
        long now = System.currentTimeMillis();
        Set<String> ids = cache.smembers(CacheKeys.SERVER_LIST);
        if (ids == null) return;
        
        for (String id : ids) {
            ServerInfo info = get(id);
            if (info == null || now - info.lastHeartbeat >= 60000) {
                cache.del(CacheKeys.server(id));
                cache.srem(CacheKeys.SERVER_LIST, id);
                app.log.info("GameServer 超时移除: " + id);
            }
        }
    }
    
    public int onServerRestart(String serverId) {
        if (serverId == null || serverId.isEmpty()) return 0;
        
        int cleared = 0;
        Set<String> roomIds = cache.smembers(CacheKeys.serverRooms(serverId));
        if (roomIds != null) {
            for (String roomId : roomIds) {
                cache.del(CacheKeys.room(roomId));
                
                Set<String> userIds = cache.smembers(CacheKeys.roomUsers(roomId));
                if (userIds != null) {
                    for (String odUserId : userIds) {
                        cache.del(CacheKeys.user(Long.parseLong(odUserId)));
                    }
                    cache.del(CacheKeys.roomUsers(roomId));
                }
                cleared++;
            }
            cache.del(CacheKeys.serverRooms(serverId));
        }
        
        app.log.info("GameServer 重启清理: serverId=" + serverId + ", cleared=" + cleared);
        return cleared;
    }
    
    private void save(ServerInfo info) {
        cache.set(CacheKeys.server(info.id), app.json.stringify(info), CacheKeys.SERVER_EXPIRE);
    }
}
