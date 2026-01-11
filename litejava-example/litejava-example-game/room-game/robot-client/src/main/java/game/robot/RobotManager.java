package game.robot;

import litejava.App;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * 机器人管理器 (BabyKylin 模式)
 * 
 * 流程:
 * 1. 调用 HallServer HTTP 接口创建/加入房间
 * 2. 获取 GameServer 地址和 token
 * 3. 连接 GameServer WebSocket 进行游戏
 */
public class RobotManager {
    
    private final App app;
    private final String hallHttpUrl;
    private final String accountPriKey;
    
    public RobotManager(App app) {
        this.app = app;
        this.hallHttpUrl = app.conf.getString("hall", "httpUrl", "http://localhost:8201");
        this.accountPriKey = app.conf.getString("hall", "accountPriKey", "ACCOUNT_PRI_KEY_2024");
    }
    
    /**
     * 创建私人房间 (BabyKylin: /create_private_room)
     * 
     * @return {roomid, ip, port, token, time, sign} 或 null
     */
    public Map<String, Object> createPrivateRoom(long userId, String name, String gameType) {
        try {
            String account = "robot_" + userId;
            String sign = md5(account + "127.0.0.1" + accountPriKey);
            
            String url = hallHttpUrl + "/create_private_room"
                + "?account=" + encode(account)
                + "&userId=" + userId
                + "&name=" + encode(name)
                + "&gameType=" + encode(gameType)
                + "&conf={}"
                + "&sign=" + sign;
            
            Map<String, Object> resp = httpGet(url);
            if (resp != null && toInt(resp.get("code")) == 0) {
                return (Map<String, Object>) resp.get("data");
            }
            app.log.error("创建房间失败: " + resp);
            return null;
        } catch (Exception e) {
            app.log.error("创建房间异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 加入私人房间 (BabyKylin: /enter_private_room)
     * 
     * @return {roomid, ip, port, token, time, sign} 或 null
     */
    public Map<String, Object> enterPrivateRoom(long userId, String name, String roomId) {
        try {
            String account = "robot_" + userId;
            String sign = md5(account + "127.0.0.1" + accountPriKey);
            
            String url = hallHttpUrl + "/enter_private_room"
                + "?account=" + encode(account)
                + "&userId=" + userId
                + "&name=" + encode(name)
                + "&roomid=" + encode(roomId)
                + "&sign=" + sign;
            
            Map<String, Object> resp = httpGet(url);
            if (resp != null && toInt(resp.get("code")) == 0) {
                return (Map<String, Object>) resp.get("data");
            }
            app.log.error("加入房间失败: " + resp);
            return null;
        } catch (Exception e) {
            app.log.error("加入房间异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 开始匹配 (扩展: /match/start)
     * 
     * @return {status, roomid, ip, port, token} 或 null
     */
    public Map<String, Object> startMatch(long userId, String name, String gameType) {
        try {
            String url = hallHttpUrl + "/match/start";
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("name", name);
            body.put("gameType", gameType);
            body.put("level", "normal");
            
            Map<String, Object> resp = httpPost(url, body);
            if (resp != null && toInt(resp.get("code")) == 0) {
                return (Map<String, Object>) resp.get("data");
            }
            app.log.error("匹配失败: " + resp);
            return null;
        } catch (Exception e) {
            app.log.error("匹配异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 取消匹配 (扩展: /match/cancel)
     */
    public void cancelMatch(long userId) {
        try {
            String url = hallHttpUrl + "/match/cancel";
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            httpPost(url, body);
        } catch (Exception e) {
            app.log.error("取消匹配异常: " + e.getMessage());
        }
    }
    
    /**
     * 构建 GameServer WebSocket URL
     */
    public String buildGameWsUrl(String ip, int port) {
        return "ws://" + ip + ":" + port + "/game";
    }
    
    // ==================== HTTP 工具 ====================
    
    private Map<String, Object> httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return app.json.parseMap(sb.toString());
        }
    }
    
    private Map<String, Object> httpPost(String urlStr, Map<String, Object> body) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        
        String json = app.json.stringify(body);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("UTF-8"));
        }
        
        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return app.json.parseMap(sb.toString());
        }
    }
    
    private String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
    
    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
    
    private int toInt(Object o) {
        if (o == null) return -1;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return -1; }
    }
}
