package orderservice.service;

import common.BizException;
import common.Err;
import orderservice.G;
import litejava.plugins.http.RpcClient;

import java.util.Map;

/**
 * 用户服务客户端
 * 
 * @author LiteJava
 */
public class UserClient {
    
    /**
     * 获取用户信息（带熔断）
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getUser(Long userId) {
        return G.circuitBreaker.execute(
            "user-service",
            () -> doGetUser(userId),
            () -> Map.of("id", userId, "username", "未知用户", "fallback", true)
        );
    }
    
    /**
     * 检查用户是否存在
     */
    public static boolean userExists(Long userId) {
        try {
            Map<String, Object> user = doGetUser(userId);
            return user != null && !user.containsKey("error");
        } catch (Exception e) {
            G.app.log.error("检查用户失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取熔断器状态
     */
    public static String getCircuitBreakerState() {
        return G.circuitBreaker.getState("user-service").name();
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> doGetUser(Long userId) {
        try {
            RpcClient rpc = G.app.getPlugin(RpcClient.class);
            rpc.discovery(G.consul());
            
            Map<String, Object> response = rpc.call("user-service", "/user/detail", Map.of("id", userId));
            
            if (response.get("code") != null && ((Number) response.get("code")).intValue() == 0) {
                return (Map<String, Object>) response.get("data");
            }
            
            return Map.of("error", response.get("msg"));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            BizException.error(Err.SERVER_ERROR, "调用 user-service 失败: " + e.getMessage());
            return null; // unreachable
        }
    }
}
