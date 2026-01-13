package game.hall.util;

import game.hall.G;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * HTTP 工具类
 */
public class HttpUtil {
    
    /**
     * URL 编码
     */
    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
    
    /**
     * GET 请求
     */
    public static String get(String urlStr) {
        return get(urlStr, null);
    }
    
    /**
     * GET 请求 (带参数)
     */
    public static String get(String urlStr, Map<String, Object> params) {
        try {
            StringBuilder sb = new StringBuilder(urlStr);
            if (params != null && !params.isEmpty()) {
                sb.append(urlStr.contains("?") ? "&" : "?");
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    sb.append(encode(e.getKey()));
                    sb.append("=");
                    sb.append(encode(String.valueOf(e.getValue())));
                    sb.append("&");
                }
            }
            String fullUrl = sb.toString();
            
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }
        } catch (Exception e) {
            if (G.app != null) {
                G.app.log.error("HTTP GET failed: " + urlStr + " - " + e.getMessage());
            }
            return null;
        }
    }
}
