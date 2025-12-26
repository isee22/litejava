package litejava.plugins.validation;

import org.junit.jupiter.api.*;

import javax.validation.ConstraintViolation;
import javax.validation.constraints.*;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationPlugin 测试 - 基于 JSR-380 Bean Validation
 */
class ValidationPluginTest {
    
    private ValidationPlugin plugin;
    
    public static class UserForm {
        @NotNull(message = "用户名不能为空")
        @Size(min = 2, max = 20, message = "用户名长度 2-20")
        public String username;
        
        @Email(message = "邮箱格式不正确")
        public String email;
        
        @Size(min = 6, message = "密码至少6位")
        public String password;
    }
    
    @BeforeEach
    void setUp() {
        plugin = new ValidationPlugin();
        plugin.factory = javax.validation.Validation.buildDefaultValidatorFactory();
        plugin.validator = plugin.factory.getValidator();
    }
    
    @AfterEach
    void tearDown() {
        if (plugin.factory != null) plugin.factory.close();
    }
    
    @Test
    void testValidForm() {
        UserForm form = new UserForm();
        form.username = "张三";
        form.email = "test@example.com";
        form.password = "123456";
        
        Set<ConstraintViolation<UserForm>> violations = plugin.validator.validate(form);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testUsernameNull() {
        UserForm form = new UserForm();
        form.username = null;
        form.email = "test@example.com";
        form.password = "123456";
        
        Set<ConstraintViolation<UserForm>> violations = plugin.validator.validate(form);
        assertFalse(violations.isEmpty());
    }
    
    @Test
    void testInvalidEmail() {
        UserForm form = new UserForm();
        form.username = "张三";
        form.email = "invalid-email";
        form.password = "123456";
        
        Set<ConstraintViolation<UserForm>> violations = plugin.validator.validate(form);
        assertFalse(violations.isEmpty());
    }
    
    @Test
    void testPasswordTooShort() {
        UserForm form = new UserForm();
        form.username = "张三";
        form.email = "test@example.com";
        form.password = "123";
        
        Set<ConstraintViolation<UserForm>> violations = plugin.validator.validate(form);
        assertFalse(violations.isEmpty());
    }
}
