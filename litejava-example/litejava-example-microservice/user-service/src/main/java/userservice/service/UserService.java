package userservice.service;

import common.BizException;
import common.Err;
import userservice.G;
import userservice.model.User;

import java.util.List;

/**
 * 用户服务
 */
public class UserService {
    
    public static List<User> findAll() {
        return G.userMapper.findAll();
    }
    
    public static User findById(Long id) {
        return G.userMapper.findById(id);
    }
    
    public static User findByUsername(String username) {
        return G.userMapper.findByUsername(username);
    }
    
    public static User create(User user) {
        if (G.userMapper.findByUsername(user.username) != null) BizException.error(Err.USER_EXISTS, "用户名已存在");
        G.userMapper.insert(user);
        return user;
    }
    
    public static User update(Long id, User user) {
        User existing = G.userMapper.findById(id);
        if (existing == null) {
            return null;
        }
        user.id = id;
        G.userMapper.update(user);
        return G.userMapper.findById(id);
    }
    
    public static boolean delete(Long id) {
        return G.userMapper.delete(id) > 0;
    }
}
