package com.example.service;

import com.example.mapper.UserMapper;
import com.example.model.User;
import litejava.plugins.database.MyBatisPlugin;

import java.util.List;

/**
 * 用户服务
 */
public class UserService {
    
    private final MyBatisPlugin mybatis;
    
    public UserService(MyBatisPlugin mybatis) {
        this.mybatis = mybatis;
    }
    
    public List<User> findAll() {
        return mybatis.execute(UserMapper.class, UserMapper::findAll);
    }
    
    public User findById(Long id) {
        return mybatis.execute(UserMapper.class, mapper -> mapper.findById(id));
    }
    
    public User findByUsername(String username) {
        return mybatis.execute(UserMapper.class, mapper -> mapper.findByUsername(username));
    }
    
    public User create(User user) {
        mybatis.execute(UserMapper.class, mapper -> mapper.insert(user));
        return user;
    }
    
    public User update(User user) {
        mybatis.execute(UserMapper.class, mapper -> mapper.update(user));
        return findById(user.id);
    }
    
    public boolean delete(Long id) {
        return mybatis.execute(UserMapper.class, mapper -> mapper.delete(id)) > 0;
    }
}
