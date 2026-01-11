# 代码规范

## 数据结构
- 禁止使用 `Map<String, Object>` 作为数据传输结构
- 使用 VO（Value Object）或 Entity 类定义明确的数据结构
- VO 放在 `vo/` 包下，Entity 放在 `entity/` 包下

## 配置数据
- 配置数据必须从数据库读取，禁止硬编码在代码中
- 可以使用缓存提高性能，但数据源必须是数据库
- 初始化数据放在 `init.sql` 中

## 错误处理
- 使用 `GameException.error(code, msg)` 抛出业务异常，而不是返回错误字符串
- 这样可以统一异常处理，简化代码

```java
// ✅ 推荐：使用 GameException
public static void acceptRequest(long userId, long requestId) {
    FriendRequestEntity request = DB.execute(...);
    if (request == null) {
        GameException.error(1, "请求不存在");
    }
    // 正常逻辑
}

// ❌ 避免：返回错误字符串
public static Result acceptRequest(long userId, long requestId) {
    FriendRequestEntity request = DB.execute(...);
    if (request == null) {
        return Result.fail("请求不存在");
    }
    // 正常逻辑
}
```

## 控制流
- 使用早返回（Early Return）模式，避免深层嵌套
- 先处理错误/边界情况，再处理正常逻辑

```java
// ✅ 推荐：早返回
private void onLogin(WsSession session, Map<String, Object> data) {
    if (room == null) {
        send(session, Cmd.LOGIN, ErrCode.ROOM_NOT_FOUND, null);
        return;
    }
    
    if (seatIndex < 0) {
        send(session, Cmd.LOGIN, ErrCode.NOT_IN_ROOM, null);
        return;
    }
    
    // 正常逻辑
    bind(userId, session);
    send(session, Cmd.LOGIN, vo);
}

// ❌ 避免：深层嵌套
private void onLogin(WsSession session, Map<String, Object> data) {
    if (room != null) {
        if (seatIndex >= 0) {
            bind(userId, session);
            send(session, Cmd.LOGIN, vo);
        } else {
            send(session, Cmd.LOGIN, ErrCode.NOT_IN_ROOM, null);
        }
    } else {
        send(session, Cmd.LOGIN, ErrCode.ROOM_NOT_FOUND, null);
    }
}
```
