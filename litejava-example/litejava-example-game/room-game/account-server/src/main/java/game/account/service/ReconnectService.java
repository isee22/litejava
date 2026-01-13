package game.account.service;

import game.account.Cache;
import game.account.vo.LoginResp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * 断线重连服务
 * 
 * 从 Redis 读取用户房间信息 (HallServer 写入)
 * 参考 BabyKylin: db.get_room_id_of_user
 */
public class ReconnectService {
    
    private static final String KEY_USER_ROOM = "hall:user:";
    private static final String KEY_ROOM_SERVER = "hall:room:";
    
    private String hallServerUrl = "http://localhost:8201";
    
    public void setHallServerUrl(String url) {
        this.hallServerUrl = url;
    }
    
    /**
     * 填充断线重连信息到登录响应
     * 
     * 会验证房间是否真的存在，如果不存在则清理残留状态
     */
    public void fillReconnectInfo(LoginResp resp) {
        String roomId = Cache.instance.get(KEY_USER_ROOM + resp.userId);
        if (roomId == null) return;
        
        String serverId = Cache.instance.get(KEY_ROOM_SERVER + roomId);
        if (serverId == null) {
            // 房间记录不存在，清理用户状态
            Cache.instance.del(KEY_USER_ROOM + resp.userId);
            return;
        }
        
        // 验证房间是否真的存在 (调用 HallServer)
        if (!isRoomRunning(roomId, serverId)) {
            // 房间已不存在，清理残留状态
            Cache.instance.del(KEY_USER_ROOM + resp.userId);
            Cache.instance.del(KEY_ROOM_SERVER + roomId);
            return;
        }
        
        // 解析 serverId 获取 ip:port
        String[] parts = serverId.split(":");
        if (parts.length >= 2) {
            resp.roomId = roomId;
            resp.serverId = serverId;
            resp.serverIp = parts[0];
            resp.serverPort = Integer.parseInt(parts[1]);
        }
    }
    
    /**
     * 检查房间是否在运行
     */
    private boolean isRoomRunning(String roomId, String serverId) {
        try {
            // serverId 格式: ip:wsPort，需要用 httpPort 调用
            // 假设 httpPort = wsPort + 1
            String[] parts = serverId.split(":");
            if (parts.length < 2) return false;
            
            String ip = parts[0];
            int wsPort = Integer.parseInt(parts[1]);
            int httpPort = wsPort + 1;
            
            String url = "http://" + ip + ":" + httpPort + "/is_room_runing?roomid=" + roomId 
                + "&sign=" + md5(roomId + "ROOM_PRI_KEY_2024");
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            int code = conn.getResponseCode();
            if (code != 200) return false;
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            conn.disconnect();
            
            // 简单解析 JSON: {"errcode":0,"runing":true}
            String json = sb.toString();
            return json.contains("\"runing\":true") || json.contains("\"runing\": true");
        } catch (Exception e) {
            return false;
        }
    }
    
    private String md5(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}

