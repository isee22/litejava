package litejava.plugins.security;

import litejava.*;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionPlugin 测试
 */
class SessionPluginTest {
    
    @Test
    @SuppressWarnings("unchecked")
    void testCreatesNewSession() throws Exception {
        Context ctx = createContext();
        
        SessionPlugin session = new SessionPlugin();
        
        session.handle(ctx, () -> {
            Map<String, Object> sess = (Map<String, Object>) ctx.state.get("session");
            assertNotNull(sess);
            sess.put("userId", 123);
        });
        
        // 验证设置了 session cookie
        String setCookie = ctx.getResponseHeaders().get("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("JSESSIONID="));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    void testReusesExistingSession() throws Exception {
        SessionPlugin session = new SessionPlugin();
        
        // 第一次请求 - 创建 session
        Context ctx1 = createContext();
        session.handle(ctx1, () -> {
            Map<String, Object> sess = (Map<String, Object>) ctx1.state.get("session");
            sess.put("counter", 1);
        });
        
        // 获取 session id
        String setCookie = ctx1.getResponseHeaders().get("Set-Cookie");
        String sessionId = extractSessionId(setCookie);
        
        // 第二次请求 - 复用 session
        Context ctx2 = createContext();
        ctx2.headers.put("Cookie", "JSESSIONID=" + sessionId);
        
        session.handle(ctx2, () -> {
            Map<String, Object> sess = (Map<String, Object>) ctx2.state.get("session");
            Integer counter = (Integer) sess.get("counter");
            assertEquals(1, counter);
            sess.put("counter", 2);
        });
    }
    
    @Test
    @SuppressWarnings("unchecked")
    void testCustomCookieName() throws Exception {
        SessionPlugin session = new SessionPlugin();
        session.cookieName = "SID";
        session.maxAge = 7200;
        
        Context ctx = createContext();
        
        session.handle(ctx, () -> {
            Map<String, Object> sess = (Map<String, Object>) ctx.state.get("session");
            sess.put("test", "value");
        });
        
        String setCookie = ctx.getResponseHeaders().get("Set-Cookie");
        assertTrue(setCookie.contains("SID="));
        assertTrue(setCookie.contains("Max-Age=7200"));
    }
    
    @Test
    void testGetSessionHelper() throws Exception {
        SessionPlugin session = new SessionPlugin();
        
        Context ctx = createContext();
        
        session.handle(ctx, () -> {
            Map<String, Object> sess = SessionPlugin.get(ctx);
            assertNotNull(sess);
            sess.put("key", "value");
            assertEquals("value", sess.get("key"));
        });
    }
    
    private Context createContext() {
        Context ctx = new Context();
        ctx.method = "GET";
        ctx.path = "/test";
        return ctx;
    }
    
    private String extractSessionId(String setCookie) {
        if (setCookie == null) return null;
        for (String part : setCookie.split(";")) {
            if (part.trim().startsWith("JSESSIONID=")) {
                return part.trim().substring("JSESSIONID=".length());
            }
        }
        return null;
    }
}
