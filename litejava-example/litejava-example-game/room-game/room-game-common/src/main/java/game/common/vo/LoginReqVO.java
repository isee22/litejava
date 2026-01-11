package game.common.vo;

/**
 * 登录请求 VO
 */
public class LoginReqVO {
    /** 用户名密码登录 */
    public String username;
    public String password;
    
    /** 兼容旧方式：直接用 userId */
    public long userId;
    public String name;
}
