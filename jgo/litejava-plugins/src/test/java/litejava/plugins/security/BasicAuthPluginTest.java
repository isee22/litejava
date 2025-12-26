package litejava.plugins.security;

import litejava.*;
import litejava.plugins.LiteJava;
import org.junit.jupiter.api.*;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BasicAuthPlugin 测试
 */
class BasicAuthPluginTest {
    
    @Test
    void testValidCredentials() throws Exception {
        Context ctx = createContext();
        String credentials = Base64.getEncoder().encodeToString("admin:password".getBytes());
        ctx.headers.put("Authorization", "Basic " + credentials);
        
        BasicAuthPlugin auth = new BasicAuthPlugin("admin", "password");
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        
        auth.handle(ctx, () -> nextCalled.set(true));
        
        assertTrue(nextCalled.get());
    }
    
    @Test
    void testInvalidCredentials() throws Exception {
        Context ctx = createContext();
        String credentials = Base64.getEncoder().encodeToString("admin:wrong".getBytes());
        ctx.headers.put("Authorization", "Basic " + credentials);
        
        App app = LiteJava.create();
        BasicAuthPlugin auth = new BasicAuthPlugin("admin", "password");
        app.use(auth);
        ctx.app = app;
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        
        auth.handle(ctx, () -> nextCalled.set(true));
        
        assertFalse(nextCalled.get());
        assertEquals(401, ctx.getResponseStatus());
    }
    
    @Test
    void testMissingAuthHeader() throws Exception {
        Context ctx = createContext();
        
        App app = LiteJava.create();
        BasicAuthPlugin auth = new BasicAuthPlugin("admin", "password");
        app.use(auth);
        ctx.app = app;
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        
        auth.handle(ctx, () -> nextCalled.set(true));
        
        assertFalse(nextCalled.get());
        assertEquals(401, ctx.getResponseStatus());
        assertEquals("Basic realm=\"Restricted\"", ctx.getResponseHeaders().get("WWW-Authenticate"));
    }
    
    @Test
    void testCustomValidator() throws Exception {
        Context ctx = createContext();
        String credentials = Base64.getEncoder().encodeToString("user:secret".getBytes());
        ctx.headers.put("Authorization", "Basic " + credentials);
        
        BasicAuthPlugin auth = new BasicAuthPlugin((user, pass) -> 
            "user".equals(user) && "secret".equals(pass)
        );
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        
        auth.handle(ctx, () -> nextCalled.set(true));
        
        assertTrue(nextCalled.get());
    }
    
    private Context createContext() {
        Context ctx = new Context();
        ctx.method = "GET";
        ctx.path = "/admin";
        return ctx;
    }
}
