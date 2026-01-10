package userservice.model;

import java.time.LocalDateTime;

/**
 * 用户实体
 * 
 * <p>对应数据库 users 表，使用 public 字段（LiteJava 风格）
 * 
 * <h2>字段说明</h2>
 * <ul>
 *   <li>id - 主键，自增</li>
 *   <li>username - 用户名，唯一</li>
 *   <li>email - 邮箱</li>
 *   <li>phone - 手机号</li>
 *   <li>status - 状态：1=正常，0=禁用</li>
 *   <li>createdAt - 创建时间</li>
 *   <li>updatedAt - 更新时间</li>
 * </ul>
 * 
 * @author LiteJava
 */
public class User {
    
    /** 用户 ID */
    public Long id;
    
    /** 用户名（唯一） */
    public String username;
    
    /** 邮箱 */
    public String email;
    
    /** 手机号 */
    public String phone;
    
    /** 状态：1=正常，0=禁用 */
    public Integer status;
    
    /** 创建时间 */
    public LocalDateTime createdAt;
    
    /** 更新时间 */
    public LocalDateTime updatedAt;
}
