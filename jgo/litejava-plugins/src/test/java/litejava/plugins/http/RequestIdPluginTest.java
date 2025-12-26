package litejava.plugins.http;

import litejava.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RequestIdPlugin 测试
 */
class RequestIdPluginTest {
    
    @Test
    void testGeneratesRequestId() throws Exception {
        Context ctx = createContext();
        
        RequestIdPlugin plugin = new RequestIdPlugin();
        
        plugin.handle(ctx, () -> {
            // 验证 state 中有 requestId
            assertNotNull(ctx.state.get("requestId"));
        });
        
        // 验证响应头
        String requestId = ctx.getResponseHeaders().get("X-Request-Id");
        assertNotNull(requestId);
        assertTrue(requestId.length() > 0);
    }
    
    @Test
    void testUsesExistingRequestId() throws Exception {
        Context ctx = createContext();
        ctx.headers.put("X-Request-Id", "existing-id-123");
        
        RequestIdPlugin plugin = new RequestIdPlugin();
        
        plugin.handle(ctx, () -> {});
        
        assertEquals("existing-id-123", ctx.getResponseHeaders().get("X-Request-Id"));
    }
    
    @Test
    void testCustomHeaderName() throws Exception {
        Context ctx = createContext();
        
        RequestIdPlugin plugin = new RequestIdPlugin();
        plugin.headerName = "X-Trace-Id";
        plugin.stateKey = "traceId";
        
        plugin.handle(ctx, () -> {});
        
        assertNotNull(ctx.getResponseHeaders().get("X-Trace-Id"));
        assertNull(ctx.getResponseHeaders().get("X-Request-Id"));
    }
    
    private Context createContext() {
        Context ctx = new Context();
        ctx.method = "GET";
        ctx.path = "/test";
        return ctx;
    }
}
