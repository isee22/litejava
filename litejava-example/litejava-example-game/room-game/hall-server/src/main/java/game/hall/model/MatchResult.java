package game.hall.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 匹配结果
 */
public class MatchResult {
    public String status;
    public String roomId;
    public String ip;         // 游戏服内网 IP
    public int port;          // 游戏服 WebSocket 端口
    public String gameType;
    public String token;
    
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", status);
        m.put("roomid", roomId);
        m.put("ip", ip);
        m.put("port", port);
        m.put("gameType", gameType);
        m.put("token", token);
        return m;
    }
}
