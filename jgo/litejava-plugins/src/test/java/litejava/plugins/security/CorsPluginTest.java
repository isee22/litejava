package litejava.plugins.security;

import litejava.*;
import litejava.plugins.LiteJava;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CorsPlugin 测试
 */
class CorsPluginTest {
    
    @Test
    void testDefaultCors() throws Exception {
        Context ctx = createContext();
        ctx.method = "GET";
        ctx.headers.put("Origin", "http://example.com");
        
        App app = LiteJava.create();
        CorsPlugin cors = new CorsPlugin();
        app.use(cors);
        ctx.app = app;
        
        cors.handle(ctx, () -> ctx.ok("success"));
        
        assertEquals("*", ctx.getResponseHeaders().get("Access-Control-Allow-Origin"));
    }
    
    @Test
    void testPreflightRequest() throws Exception {
        Context ctx = createContext();
        ctx.method = "OPTIONS";
        ctx.headers.put("Origin", "http://example.com");
        ctx.headers.put("Access-Control-Request-Method", "POST");
        
        CorsPlugin cors = new CorsPlugin();
        
        cors.handle(ctx, () -> {
            fail("Preflight should not call next");
        });
        
        assertEquals(204, ctx.getResponseStatus());
        assertNotNull(ctx.getResponseHeaders().get("Access-Control-Allow-Methods"));
    }
    
    @Test
    void testCustomOrigin() throws Exception {
        Context ctx = createContext();
        ctx.headers.put("Origin", "http://mysite.com");
        
        CorsPlugin cors = new CorsPlugin("http://mysite.com");
        
        cors.handle(ctx, () -> {});
        
        assertEquals("http://mysite.com", ctx.getResponseHeaders().get("Access-Control-Allow-Origin"));
    }
    
    private Context createContext() {
        Context ctx = new Context();
        ctx.method = "GET";
        ctx.path = "/test";
        return ctx;
    }
}
