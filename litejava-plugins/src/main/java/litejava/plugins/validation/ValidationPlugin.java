package litejava.plugins.validation;

import litejava.Plugin;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * 参数校验插件 - 基于 JSR-380 (Bean Validation 2.0)
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.hibernate.validator</groupId>
 *     <artifactId>hibernate-validator</artifactId>
 *     <version>6.2.5.Final</version>
 * </dependency>
 * <dependency>
 *     <groupId>org.glassfish</groupId>
 *     <artifactId>jakarta.el</artifactId>
 *     <version>3.0.4</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 定义带注解的 Bean
 * public class UserForm {
 *     @NotNull(message = "用户名不能为空")
 *     @Size(min = 2, max = 20, message = "用户名长度 2-20")
 *     public String username;
 *     
 *     @Email(message = "邮箱格式不正确")
 *     public String email;
 * }
 * 
 * // 注册插件
 * app.use(new ValidationPlugin());
 * 
 * // 在 Handler 中验证
 * app.post("/users", ctx -> {
 *     UserForm form = ctx.bodyAs(UserForm.class);
 *     
 *     Set<ConstraintViolation<UserForm>> violations = ValidationPlugin.instance.validator.validate(form);
 *     if (!violations.isEmpty()) {
 *         List<String> errors = violations.stream()
 *             .map(v -> v.getPropertyPath() + ": " + v.getMessage())
 *             .collect(Collectors.toList());
 *         ctx.status(400).json(Map.of("errors", errors));
 *         return;
 *     }
 *     // ...
 * });
 * }</pre>
 * 
 * <h2>常用注解</h2>
 * <ul>
 *   <li>{@code @NotNull} - 非空</li>
 *   <li>{@code @NotEmpty} - 非空且长度 > 0</li>
 *   <li>{@code @NotBlank} - 非空且去空格后长度 > 0</li>
 *   <li>{@code @Size(min, max)} - 长度范围</li>
 *   <li>{@code @Min(value)} / {@code @Max(value)} - 数值范围</li>
 *   <li>{@code @Email} - 邮箱格式</li>
 *   <li>{@code @Pattern(regexp)} - 正则匹配</li>
 *   <li>{@code @Past} / {@code @Future} - 日期校验</li>
 * </ul>
 */
public class ValidationPlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static ValidationPlugin instance;
    
    /** ValidatorFactory */
    public ValidatorFactory factory;
    
    /** JSR-380 Validator */
    public Validator validator;
    
    public ValidationPlugin() {
        instance = this;
    }
    
    @Override
    public void config() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Override
    public void uninstall() {
        if (factory != null) factory.close();
    }
}
