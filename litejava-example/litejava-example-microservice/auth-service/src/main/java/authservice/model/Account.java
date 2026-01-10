package authservice.model;

import java.time.LocalDateTime;

public class Account {
    public Long id;
    public Long userId;
    public String username;
    public String passwordHash;
    public Integer status;
    public LocalDateTime lastLoginAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
