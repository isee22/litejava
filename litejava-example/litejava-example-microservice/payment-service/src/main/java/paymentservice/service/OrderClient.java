package paymentservice.service;

import common.BizException;
import common.Err;
import paymentservice.G;
import litejava.plugins.microservice.DiscoveryPlugin.ServiceInstance;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 订单服务客户端
 * 
 * @author LiteJava
 */
public class OrderClient {
    
    /**
     * 更新订单状态（带熔断）
     */
    public static boolean updateOrderStatus(String orderNo, Integer status) {
        return G.circuitBreaker.execute(
            "order-service",
            () -> doUpdateOrderStatus(orderNo, status),
            () -> false
        );
    }
    
    @SuppressWarnings("unchecked")
    private static boolean doUpdateOrderStatus(String orderNo, Integer status) {
        ServiceInstance inst = G.consul().getInstance("order-service");
        if (inst == null) BizException.error(Err.SERVER_ERROR, "order-service 无可用实例");
        
        String url = inst.getUrl() + "/order/updateStatus";
        
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String body = "{\"orderNo\":\"" + orderNo + "\",\"status\":" + status + "}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            
            int code = conn.getResponseCode();
            if (code != 200) BizException.error(Err.SERVER_ERROR, "调用 order-service 失败: HTTP " + code);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                
                Map<String, Object> response = G.app.json.parseMap(sb.toString());
                return response.get("code") != null && ((Number) response.get("code")).intValue() == 0;
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            BizException.error(Err.SERVER_ERROR, "调用 order-service 失败: " + e.getMessage());
            return false; // unreachable
        }
    }
}
