# Common Module

公共模块，包含所有服务共享的代码。

## 内容

### 异常处理

```java
// 业务异常
public class BizException extends RuntimeException {
    public int code;
    public String msg;
    
    public static void paramRequired(String param);
    public static void paramInvalid(String msg);
    public static void error(int code, String msg);
}

// 错误码定义
public class Err {
    public static final int PARAM_REQUIRED = 400;
    public static final int PARAM_INVALID = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int PRODUCT_NOT_FOUND = 1001;
    public static final int STOCK_NOT_ENOUGH = 1002;
    public static final int ORDER_NOT_FOUND = 2001;
}
```

### 响应包装

```java
// 列表响应
public class ListResult<T> {
    public List<T> list;
    public int total;
    
    public static <T> ListResult<T> of(List<T> list);
}
```

## 使用

其他服务模块依赖此模块：

```xml
<dependency>
    <groupId>litejava</groupId>
    <artifactId>common</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 扩展

如需添加公共代码：

1. 在 `common/src/main/java/common/` 下添加类
2. 重新构建: `mvn install -pl common`
3. 其他服务自动获取更新
