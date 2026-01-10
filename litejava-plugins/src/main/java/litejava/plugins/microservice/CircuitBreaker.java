package litejava.plugins.microservice;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 熔断器 (基于 Resilience4j)
 * 
 * <p>封装 Resilience4j CircuitBreaker，提供简化的 API。
 * 
 * <h2>Maven 依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.github.resilience4j</groupId>
 *     <artifactId>resilience4j-circuitbreaker</artifactId>
 *     <version>1.7.1</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * CircuitBreaker breaker = new CircuitBreaker();
 * 
 * // 基本调用
 * Map<String, Object> result = breaker.execute("order-service", () -> {
 *     return client.get("order-service", "/orders/123");
 * });
 * 
 * // 带降级
 * Map<String, Object> result = breaker.execute("order-service", 
 *     () -> client.get("order-service", "/orders/123"),
 *     () -> Map.of("error", "Service unavailable")
 * );
 * }</pre>
 * 
 * <h2>状态说明</h2>
 * <ul>
 *   <li>CLOSED - 正常状态，请求正常通过</li>
 *   <li>OPEN - 熔断状态，请求直接失败或走降级</li>
 *   <li>HALF_OPEN - 半开状态，允许部分请求通过测试</li>
 * </ul>
 */
public class CircuitBreaker {
    
    /** 失败率阈值 (百分比，默认 50%) */
    public float failureRateThreshold = 50;
    
    /** 慢调用率阈值 (百分比，默认 100% 即不启用) */
    public float slowCallRateThreshold = 100;
    
    /** 慢调用时间阈值 (毫秒) */
    public long slowCallDurationThreshold = 60000;
    
    /** 熔断持续时间 (秒) */
    public long waitDurationInOpenState = 60;
    
    /** 半开状态允许的调用数 */
    public int permittedNumberOfCallsInHalfOpenState = 10;
    
    /** 滑动窗口大小 */
    public int slidingWindowSize = 100;
    
    /** 最小调用数（达到此数量才计算失败率） */
    public int minimumNumberOfCalls = 10;
    
    private CircuitBreakerRegistry registry;
    
    public CircuitBreaker() {
        initRegistry();
    }
    
    private void initRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .slowCallRateThreshold(slowCallRateThreshold)
            .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThreshold))
            .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenState))
            .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(minimumNumberOfCalls)
            .build();
        
        registry = CircuitBreakerRegistry.of(config);
    }
    
    /**
     * 执行带熔断保护的调用
     * @param name 熔断器名称（通常是服务名）
     * @param action 要执行的操作
     * @return 操作结果
     */
    public <T> T execute(String name, Supplier<T> action) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = registry.circuitBreaker(name);
        return cb.executeSupplier(action);
    }
    
    /**
     * 执行带熔断保护的调用（带降级）
     * @param name 熔断器名称
     * @param action 要执行的操作
     * @param fallback 降级处理
     * @return 操作结果或降级结果
     */
    public <T> T execute(String name, Supplier<T> action, Supplier<T> fallback) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = registry.circuitBreaker(name);
        try {
            return cb.executeSupplier(action);
        } catch (Exception e) {
            return fallback.get();
        }
    }
    
    /**
     * 获取熔断器状态
     */
    public State getState(String name) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = registry.circuitBreaker(name);
        switch (cb.getState()) {
            case CLOSED: return State.CLOSED;
            case OPEN: return State.OPEN;
            case HALF_OPEN: return State.HALF_OPEN;
            case DISABLED: return State.DISABLED;
            case FORCED_OPEN: return State.FORCED_OPEN;
            default: return State.CLOSED;
        }
    }
    
    /**
     * 重置熔断器
     */
    public void reset(String name) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = registry.circuitBreaker(name);
        cb.reset();
    }
    
    /**
     * 获取原生 CircuitBreaker（高级用法）
     */
    public io.github.resilience4j.circuitbreaker.CircuitBreaker getCircuitBreaker(String name) {
        return registry.circuitBreaker(name);
    }
    
    /**
     * 获取原生 Registry（高级用法）
     */
    public CircuitBreakerRegistry getRegistry() {
        return registry;
    }
    
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN,
        DISABLED,
        FORCED_OPEN
    }
}
