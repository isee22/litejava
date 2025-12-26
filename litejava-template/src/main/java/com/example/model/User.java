package com.example.model;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
public class User {
    public Long id;
    public String username;
    public String email;
    public String password;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    
    public User() {}
    
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
