package game.common;

/**
 * 游戏异常 - 带错误码，框架自动发送给客户端
 */
public class GameException extends RuntimeException {
    
    public final int code;
    
    public GameException(int code) {
        this.code = code;
    }
    
    public GameException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public static void error(int code) {
        throw new GameException(code);
    }
    
    public static void error(int code, String message) {
        throw new GameException(code, message);
    }
}
