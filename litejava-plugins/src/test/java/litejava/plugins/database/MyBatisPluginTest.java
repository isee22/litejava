package litejava.plugins.database;

import litejava.App;
import litejava.plugins.config.YamlConfPlugin;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBatisPlugin 使用示例和测试
 */
class MyBatisPluginTest {
    
    static App app;
    static MyBatisPlugin mybatis;
    
    // ========== 实体类 (使用 public fields) ==========
    
    public static class User {
        public Long id;
        public String name;
        public String email;
        
        public User() {}
        
        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
    
    // ========== Mapper 接口 ==========
    
    public interface UserMapper {
        
        @Select("SELECT * FROM users WHERE id = #{id}")
        User findById(Long id);
        
        @Select("SELECT * FROM users")
        List<User> findAll();
        
        @Select("SELECT * FROM users WHERE name = #{name}")
        User findByName(String name);
        
        @Insert("INSERT INTO users (name, email) VALUES (#{name}, #{email})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(User user);
        
        @Update("UPDATE users SET name = #{name}, email = #{email} WHERE id = #{id}")
        int update(User user);
        
        @Delete("DELETE FROM users WHERE id = #{id}")
        int delete(Long id);
    }
    
    @BeforeAll
    static void setup() throws Exception {
        app = new App();
        app.use(new YamlConfPlugin("src/test/resources/mybatis-test.yml"));
        mybatis = new MyBatisPlugin();
        app.use(mybatis);
        
        // 注册 Mapper
        mybatis.addMapper(UserMapper.class);
        
        // 创建表
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            session.getConnection().createStatement().execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(100)," +
                "  email VARCHAR(100)" +
                ")"
            );
            session.commit();
        }
    }
    
    @AfterAll
    static void teardown() {
        if (app != null) {
            app.stop();
        }
    }
    
    @BeforeEach
    void cleanTable() throws Exception {
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            session.getConnection().createStatement().execute("DELETE FROM users");
            session.commit();
        }
    }
    
    // ========== 测试用例 ==========
    
    @Test
    void testInsertAndFind() {
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 插入
            User user = new User("张三", "zhangsan@example.com");
            int rows = mapper.insert(user);
            session.commit();
            
            assertEquals(1, rows);
            assertNotNull(user.id);
            
            // 查询
            User found = mapper.findById(user.id);
            assertNotNull(found);
            assertEquals("张三", found.name);
            assertEquals("zhangsan@example.com", found.email);
        }
    }
    
    @Test
    void testUpdate() {
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 插入
            User user = new User("李四", "lisi@example.com");
            mapper.insert(user);
            session.commit();
            
            // 更新
            user.name = "李四改";
            user.email = "lisi_new@example.com";
            int rows = mapper.update(user);
            session.commit();
            
            assertEquals(1, rows);
            
            // 验证
            User found = mapper.findById(user.id);
            assertEquals("李四改", found.name);
            assertEquals("lisi_new@example.com", found.email);
        }
    }
    
    @Test
    void testDelete() {
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 插入
            User user = new User("王五", "wangwu@example.com");
            mapper.insert(user);
            session.commit();
            
            // 删除
            int rows = mapper.delete(user.id);
            session.commit();
            assertEquals(1, rows);
            
            // 验证
            User found = mapper.findById(user.id);
            assertNull(found);
        }
    }
    
    @Test
    void testFindAll() {
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 插入多条
            mapper.insert(new User("用户1", "user1@example.com"));
            mapper.insert(new User("用户2", "user2@example.com"));
            mapper.insert(new User("用户3", "user3@example.com"));
            session.commit();
            
            // 查询全部
            List<User> users = mapper.findAll();
            assertEquals(3, users.size());
        }
    }
    
    @Test
    void testTransaction_commit() {
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            mapper.insert(new User("事务用户1", "tx1@example.com"));
            mapper.insert(new User("事务用户2", "tx2@example.com"));
            session.commit();
            
            List<User> users = mapper.findAll();
            assertEquals(2, users.size());
        }
    }
    
    @Test
    void testTransaction_rollback() {
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            mapper.insert(new User("回滚用户", "rollback@example.com"));
            // 不 commit，直接关闭 = 回滚
            session.rollback();
        }
        
        // 验证回滚
        try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            List<User> users = mapper.findAll();
            assertEquals(0, users.size());
        }
    }
}
