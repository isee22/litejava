package searchservice.service;

import common.BizException;
import common.Err;
import searchservice.G;
import litejava.plugins.microservice.DiscoveryPlugin.ServiceInstance;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 商品服务客户端
 */
public class ProductClient {
    
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getAllProducts() {
        return G.circuitBreaker.execute(
            "product-service",
            ProductClient::doGetAllProducts,
            () -> Collections.emptyList()
        );
    }
    
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> doGetAllProducts() {
        ServiceInstance instance = G.consul().getInstance("product-service");
        if (instance == null) BizException.error(Err.SERVER_ERROR, "product-service 无可用实例");
        
        String url = instance.getUrl() + "/product/list";
        
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write("{}".getBytes(StandardCharsets.UTF_8));
            }
            
            if (conn.getResponseCode() != 200) BizException.error(Err.SERVER_ERROR, "调用 product-service 失败: HTTP " + conn.getResponseCode());
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                Map<String, Object> result = G.app.json.parseMap(sb.toString());
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                return (List<Map<String, Object>>) data.get("list");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            BizException.error(Err.SERVER_ERROR, "调用 product-service 失败: " + e.getMessage());
            return null; // unreachable
        }
    }
}
