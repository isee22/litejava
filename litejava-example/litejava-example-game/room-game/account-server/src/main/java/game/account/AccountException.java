package game.account;

/**
 * 账号服务异常
 */
public class AccountException extends RuntimeException {
    
    public int code;
    public String msg;
    
    public AccountException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
    
    public static void error(int code, String msg) {
        throw new AccountException(code, msg);
    }
    
    public static void error(String msg) {
        throw new AccountException(1, msg);
    }
}
