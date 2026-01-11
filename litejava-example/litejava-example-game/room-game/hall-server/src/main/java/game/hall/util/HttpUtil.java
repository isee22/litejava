package game.hall.util;

import litejava.App;

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
    
    private final App app;
    
    public HttpUtil(App app) {
        this.app = app;
    }
    
    public String get(String urlStr, Map<String, Object> params) {
        try {
            StringBuilder sb = new StringBuilder(urlStr);
            if (params != null && !params.isEmpty()) {
                sb.append("?");
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    sb.append(URLEncoder.encode(e.getKey(), "UTF-8"));
                    sb.append("=");
                    sb.append(URLEncoder.encode(String.valueOf(e.getValue()), "UTF-8"));
                    sb.append("&");
                }
            }
            URL url = new URL(sb.toString());
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
            app.log.error("HTTP GET failed: " + e.getMessage());
            return null;
        }
    }
}
