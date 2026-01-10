package userservice.controller;

import common.BizException;
import common.Err;
import common.vo.ListResult;
import litejava.Context;
import litejava.Routes;
import userservice.model.User;
import userservice.service.UserService;

import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
public class UserController {
    
    public static Routes routes() {
        return new Routes()
            .post("/user/list", UserController::list)
            .post("/user/detail", UserController::detail)
            .post("/user/create", UserController::create)
            .post("/user/update", UserController::update)
            .post("/user/delete", UserController::delete)
            .end();
    }
    
    static void list(Context ctx) {
        List<User> users = UserService.findAll();
        ctx.ok(ListResult.of(users));
    }
    
    static void detail(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null) BizException.paramRequired("id");
        
        Long id = ((Number) body.get("id")).longValue();
        User user = UserService.findById(id);
        if (user == null) BizException.error(Err.USER_NOT_FOUND, "用户不存在");
        
        ctx.ok(user);
    }
    
    static void create(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        
        String username = (String) body.get("username");
        String email = (String) body.get("email");
        
        if (username == null || username.isEmpty()) BizException.paramRequired("username");
        if (email == null || email.isEmpty()) BizException.paramRequired("email");
        
        User user = new User();
        user.username = username;
        user.email = email;
        user.phone = (String) body.get("phone");
        
        User created = UserService.create(user);
        ctx.ok(created);
    }
    
    static void update(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null) BizException.paramRequired("id");
        
        Long id = ((Number) body.get("id")).longValue();
        
        User user = new User();
        user.email = (String) body.get("email");
        user.phone = (String) body.get("phone");
        
        User updated = UserService.update(id, user);
        if (updated == null) BizException.error(Err.USER_NOT_FOUND, "用户不存在");
        
        ctx.ok(updated);
    }
    
    static void delete(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null) BizException.paramRequired("id");
        
        Long id = ((Number) body.get("id")).longValue();
        if (!UserService.delete(id)) BizException.error(Err.USER_NOT_FOUND, "用户不存在");
        
        ctx.ok();
    }
}
