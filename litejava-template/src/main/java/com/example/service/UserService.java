package com.example.service;

import com.example.mapper.UserMapper;
import com.example.model.User;
import litejava.plugins.database.MyBatisPlugin;

import java.util.List;

/**
 * 用户服务
 */
public class UserService {
    
    public static final UserService instance = new UserService();
    
    public List<User> findAll() {
        return MyBatisPlugin.instance.execute(UserMapper.class, UserMapper::findAll);
    }
    
    public User findById(Long id) {
        return MyBatisPlugin.instance.execute(UserMapper.class, mapper -> mapper.findById(id));
    }
    
    public User findByUsername(String username) {
        return MyBatisPlugin.instance.execute(UserMapper.class, mapper -> mapper.findByUsername(username));
    }
    
    public User create(User user) {
        MyBatisPlugin.instance.execute(UserMapper.class, mapper -> mapper.insert(user));
        return user;
    }
    
    public User update(User user) {
        MyBatisPlugin.instance.execute(UserMapper.class, mapper -> mapper.update(user));
        return findById(user.id);
    }
    
    public boolean delete(Long id) {
        return MyBatisPlugin.instance.execute(UserMapper.class, mapper -> mapper.delete(id)) > 0;
    }
}
