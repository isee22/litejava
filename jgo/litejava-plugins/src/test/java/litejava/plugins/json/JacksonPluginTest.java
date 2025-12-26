package litejava.plugins.json;

import litejava.*;
import org.junit.jupiter.api.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JacksonPlugin 测试
 */
class JacksonPluginTest {
    
    JacksonPlugin jackson;
    
    @BeforeEach
    void setup() {
        jackson = new JacksonPlugin();
        App app = new App();
        app.use(jackson);
    }
    
    @Test
    void testStringifyMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "张三");
        data.put("age", 25);
        data.put("active", true);
        
        String json = jackson.stringify(data);
        
        assertTrue(json.contains("\"name\":\"张三\""));
        assertTrue(json.contains("\"age\":25"));
        assertTrue(json.contains("\"active\":true"));
    }
    
    @Test
    void testStringifyObject() {
        User user = new User();
        user.id = 1L;
        user.name = "李四";
        user.email = "lisi@example.com";
        
        String json = jackson.stringify(user);
        
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"李四\""));
        assertTrue(json.contains("\"email\":\"lisi@example.com\""));
    }
    
    @Test
    void testParseToClass() {
        String json = "{\"id\":1,\"name\":\"王五\",\"email\":\"wangwu@example.com\"}";
        
        User user = jackson.parse(json, User.class);
        
        assertEquals(1L, user.id);
        assertEquals("王五", user.name);
        assertEquals("wangwu@example.com", user.email);
    }
    
    @Test
    void testParseToMap() {
        String json = "{\"code\":0,\"msg\":\"success\",\"data\":{\"count\":10}}";
        
        Map<String, Object> result = jackson.parseMap(json);
        
        assertEquals(0, result.get("code"));
        assertEquals("success", result.get("msg"));
        assertTrue(result.get("data") instanceof Map);
    }
    
    @Test
    void testNullHandling() {
        User user = new User();
        user.id = 1L;
        user.name = null;
        
        String json = jackson.stringify(user);
        
        // Jackson 默认包含 null
        assertTrue(json.contains("\"name\":null") || !json.contains("\"name\""));
    }
    
    @Test
    void testEmptyJson() {
        Map<String, Object> result = jackson.parseMap("{}");
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    // 测试用实体类
    public static class User {
        public Long id;
        public String name;
        public String email;
    }
}
