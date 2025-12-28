# 编码规范

## 插件配置规范

插件从配置文件读取的参数必须：

1. 定义为 public 字段，便于代码直接设置
2. 在 config() 方法开头集中加载所有配置
3. 字段名与配置 key 保持一致

```java
// ✅ 正确示例
public class JaxRsPlugin extends Plugin {
    // 配置字段（可代码设置，也可配置文件覆盖）
    public String packages;
    public boolean autoScan = true;
    
    @Override
    public void config() {
        // 集中加载配置
        packages = app.conf.getString("jaxrs", "packages", packages);
        autoScan = app.conf.getBool("jaxrs", "autoScan", autoScan);
        
        // 业务逻辑
        if (packages != null) {
            scanPackage(packages);
        }
    }
}

// ❌ 错误示例 - 配置分散在各处
public void config() {
    String packages = app.conf.getString("jaxrs", "packages", null);  // 局部变量
    // ...
    boolean autoScan = app.conf.getBool("jaxrs", "autoScan", true);   // 分散加载
}
```

## 代码质量

- 有问题直接解决根本原因，不要写"修复"或"确保"类的补丁代码
- 不要用 workaround，找到问题源头修正

## 命名规范

- 类名使用 PascalCase
- 方法名和变量名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 包名全小写

## 代码风格

- 使用 public 字段代替 getter/setter
- 优先使用 `Map<String, Object>` 传递数据
- 使用函数式接口和 Lambda
- 框架异常继承 `LiteJavaException`
