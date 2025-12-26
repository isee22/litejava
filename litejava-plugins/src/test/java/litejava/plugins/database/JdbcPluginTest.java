package litejava.plugins.database;

import litejava.App;
import litejava.plugins.config.YamlConfPlugin;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JdbcPlugin 测试 - 包含事务测试
 */
class JdbcPluginTest {
    
    static App app;
    static JdbcPlugin jdbc;
    
    @BeforeAll
    static void setup() {
        app = new App();
        app.use(new YamlConfPlugin("src/test/resources/jdbc-test.yml"));
        jdbc = new JdbcPlugin("db");
        app.use(jdbc);
        
        // 创建测试表
        jdbc.jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS test_users (" +
            "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "  name VARCHAR(100)," +
            "  balance INT DEFAULT 0" +
            ")"
        );
    }
    
    @AfterAll
    static void teardown() {
        if (jdbc != null) {
            jdbc.jdbcTemplate.execute("DROP TABLE IF EXISTS test_users");
        }
        if (app != null) app.stop();
    }
    
    @BeforeEach
    void clean() {
        jdbc.jdbcTemplate.execute("DELETE FROM test_users");
    }
    
    // ========== 基本 CRUD ==========
    
    @Test
    void testInsert() {
        int rows = jdbc.jdbcTemplate.update(
            "INSERT INTO test_users (name, balance) VALUES (?, ?)", 
            "张三", 100
        );
        assertEquals(1, rows);
    }
    
    @Test
    void testQuery() {
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "李四", 200);
        
        List<Map<String, Object>> users = jdbc.jdbcTemplate.queryForList("SELECT * FROM test_users");
        assertEquals(1, users.size());
        assertEquals("李四", users.get(0).get("name"));
        assertEquals(200, users.get(0).get("balance"));
    }
    
    @Test
    void testUpdate() {
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "王五", 300);
        
        int rows = jdbc.jdbcTemplate.update("UPDATE test_users SET balance = ? WHERE name = ?", 500, "王五");
        assertEquals(1, rows);
        
        Integer balance = jdbc.jdbcTemplate.queryForObject(
            "SELECT balance FROM test_users WHERE name = ?", Integer.class, "王五"
        );
        assertEquals(500, balance);
    }
    
    @Test
    void testDelete() {
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "赵六", 400);
        
        int rows = jdbc.jdbcTemplate.update("DELETE FROM test_users WHERE name = ?", "赵六");
        assertEquals(1, rows);
        
        List<Map<String, Object>> users = jdbc.jdbcTemplate.queryForList("SELECT * FROM test_users");
        assertEquals(0, users.size());
    }
    
    // ========== 事务测试 ==========
    
    @Test
    void testTransaction_commit() {
        jdbc.txTemplate.execute(status -> {
            jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "事务用户1", 100);
            jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "事务用户2", 200);
            return null;
        });
        
        List<Map<String, Object>> users = jdbc.jdbcTemplate.queryForList("SELECT * FROM test_users");
        assertEquals(2, users.size());
    }
    
    @Test
    void testTransaction_rollback() {
        // 先插入一条数据
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "原始用户", 1000);
        
        try {
            jdbc.txTemplate.execute(status -> {
                jdbc.jdbcTemplate.update("UPDATE test_users SET balance = ? WHERE name = ?", 500, "原始用户");
                // 模拟异常
                throw new RuntimeException("模拟业务异常");
            });
            fail("应该抛出异常");
        } catch (RuntimeException e) {
            assertEquals("模拟业务异常", e.getMessage());
        }
        
        // 验证回滚 - 余额应该还是 1000
        Integer balance = jdbc.jdbcTemplate.queryForObject(
            "SELECT balance FROM test_users WHERE name = ?", Integer.class, "原始用户"
        );
        assertEquals(1000, balance);
    }
    
    @Test
    void testTransaction_transfer() {
        // 模拟转账场景
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "A", 1000);
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "B", 0);
        
        // 转账 300
        jdbc.txTemplate.execute(status -> {
            jdbc.jdbcTemplate.update("UPDATE test_users SET balance = balance - ? WHERE name = ?", 300, "A");
            jdbc.jdbcTemplate.update("UPDATE test_users SET balance = balance + ? WHERE name = ?", 300, "B");
            return null;
        });
        
        Integer balanceA = jdbc.jdbcTemplate.queryForObject("SELECT balance FROM test_users WHERE name = ?", Integer.class, "A");
        Integer balanceB = jdbc.jdbcTemplate.queryForObject("SELECT balance FROM test_users WHERE name = ?", Integer.class, "B");
        
        assertEquals(700, balanceA);
        assertEquals(300, balanceB);
    }
    
    @Test
    void testTransaction_transferRollback() {
        // 模拟转账失败回滚
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "C", 1000);
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "D", 0);
        
        try {
            jdbc.txTemplate.execute(status -> {
                jdbc.jdbcTemplate.update("UPDATE test_users SET balance = balance - ? WHERE name = ?", 300, "C");
                // 模拟转账过程中出错
                if (true) throw new RuntimeException("转账失败");
                jdbc.jdbcTemplate.update("UPDATE test_users SET balance = balance + ? WHERE name = ?", 300, "D");
                return null;
            });
        } catch (RuntimeException ignored) {}
        
        // 验证回滚 - 两个账户余额都不变
        Integer balanceC = jdbc.jdbcTemplate.queryForObject("SELECT balance FROM test_users WHERE name = ?", Integer.class, "C");
        Integer balanceD = jdbc.jdbcTemplate.queryForObject("SELECT balance FROM test_users WHERE name = ?", Integer.class, "D");
        
        assertEquals(1000, balanceC);
        assertEquals(0, balanceD);
    }
    
    // ========== 事务返回值 ==========
    
    @Test
    void testTransaction_returnValue() {
        Integer count = jdbc.txTemplate.execute(status -> {
            jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "返回值测试", 100);
            return jdbc.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_users", Integer.class);
        });
        
        assertNotNull(count);
        assertEquals(1, count);
    }
    
    // ========== 手动回滚 ==========
    
    @Test
    void testTransaction_manualRollback() {
        jdbc.jdbcTemplate.update("INSERT INTO test_users (name, balance) VALUES (?, ?)", "手动回滚", 500);
        
        jdbc.txTemplate.execute(status -> {
            jdbc.jdbcTemplate.update("UPDATE test_users SET balance = ? WHERE name = ?", 0, "手动回滚");
            // 手动标记回滚
            status.setRollbackOnly();
            return null;
        });
        
        // 验证回滚
        Integer balance = jdbc.jdbcTemplate.queryForObject(
            "SELECT balance FROM test_users WHERE name = ?", Integer.class, "手动回滚"
        );
        assertEquals(500, balance);
    }
}
