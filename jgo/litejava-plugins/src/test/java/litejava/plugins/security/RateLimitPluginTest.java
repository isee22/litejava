package litejava.plugins.security;

import litejava.*;
import litejava.plugins.LiteJava;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimitPlugin 测试
 */
class RateLimitPluginTest {
    
    @Test
    void testAllowsWithinLimit() throws Exception {
        RateLimitPlugin limiter = new RateLimitPlugin(10, 1000); // 10 req/sec
        
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < 5; i++) {
            Context ctx = createContext("127.0.0.1");
            limiter.handle(ctx, () -> successCount.incrementAndGet());
        }
        
        assertEquals(5, successCount.get());
    }
    
    @Test
    void testBlocksOverLimit() throws Exception {
        App app = LiteJava.create();
        RateLimitPlugin limiter = new RateLimitPlugin(3, 1000); // 3 req/sec
        app.use(limiter);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);
        
        for (int i = 0; i < 10; i++) {
            Context ctx = createContext("127.0.0.1");
            ctx.app = app;
            limiter.handle(ctx, () -> successCount.incrementAndGet());
            if (ctx.getResponseStatus() == 429) {
                blockedCount.incrementAndGet();
            }
        }
        
        assertEquals(3, successCount.get());
        assertEquals(7, blockedCount.get());
    }
    
    @Test
    void testDifferentIpsHaveSeparateLimits() throws Exception {
        RateLimitPlugin limiter = new RateLimitPlugin(2, 1000);
        
        AtomicInteger successCount = new AtomicInteger(0);
        
        // IP1: 2 requests
        for (int i = 0; i < 2; i++) {
            Context ctx = createContext("192.168.1.1");
            limiter.handle(ctx, () -> successCount.incrementAndGet());
        }
        
        // IP2: 2 requests
        for (int i = 0; i < 2; i++) {
            Context ctx = createContext("192.168.1.2");
            limiter.handle(ctx, () -> successCount.incrementAndGet());
        }
        
        assertEquals(4, successCount.get());
    }
    
    private Context createContext(String ip) {
        Context ctx = new Context();
        ctx.method = "GET";
        ctx.path = "/test";
        ctx.remoteAddr = ip;
        return ctx;
    }
}
