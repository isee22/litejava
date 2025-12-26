package litejava.plugins.http;

import litejava.*;
import litejava.plugin.JsonPlugin;
import litejava.plugin.LogPlugin;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RecoveryPlugin 测试
 */
class RecoveryPluginTest {
    
    @Test
    void testCatchException() throws Exception {
        // 模拟 Context
        Context ctx = createContext();
        AtomicBoolean caught = new AtomicBoolean(false);
        
        RecoveryPlugin recovery = new RecoveryPlugin((c, e) -> {
            caught.set(true);
            c.status(500).json(Map.of("error", e.getMessage()));
        });
        
        // 执行会抛异常的 next
        recovery.handle(ctx, () -> {
            throw new RuntimeException("测试异常");
        });
        
        assertTrue(caught.get());
        assertEquals(500, ctx.getResponseStatus());
    }
    
    @Test
    void testNoExceptionPassThrough() throws Exception {
        Context ctx = createContext();
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        
        RecoveryPlugin recovery = new RecoveryPlugin();
        
        recovery.handle(ctx, () -> {
            nextCalled.set(true);
            ctx.ok("success");
        });
        
        assertTrue(nextCalled.get());
        assertEquals(200, ctx.getResponseStatus());
    }
    
    @Test
    void testWithStack() throws Exception {
        Context ctx = createContext();
        ctx.app = new App();
        ctx.app.devMode = false;
        ctx.app.json = new SimpleJsonPlugin();
        
        RecoveryPlugin recovery = RecoveryPlugin.withStack();
        
        recovery.handle(ctx, () -> {
            throw new IllegalArgumentException("参数错误");
        });
        
        assertEquals(500, ctx.getResponseStatus());
        String body = new String(ctx.getResponseBody());
        assertTrue(body.contains("stack"));
        assertTrue(body.contains("IllegalArgumentException"));
    }
    
    private Context createContext() {
        Context ctx = new Context();
        ctx.method = "GET";
        ctx.path = "/test";
        ctx.app = new App();
        ctx.app.json = new SimpleJsonPlugin();
        ctx.app.log = new LogPlugin();
        return ctx;
    }
    
    // 简单的 JSON 实现用于测试
    static class SimpleJsonPlugin extends JsonPlugin {
        @Override
        public String stringify(Object obj) {
            if (obj instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                Map<?, ?> map = (Map<?, ?>) obj;
                boolean first = true;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append("\"").append(e.getKey()).append("\":");
                    if (e.getValue() instanceof String) {
                        sb.append("\"").append(e.getValue()).append("\"");
                    } else {
                        sb.append(e.getValue());
                    }
                    first = false;
                }
                sb.append("}");
                return sb.toString();
            }
            return obj.toString();
        }
    }
}
