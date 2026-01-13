package game.common;

import litejava.exception.LiteJavaException;

/**
 * 游戏异常 - 带错误码，框架自动处理
 */
public class GameException extends LiteJavaException {
    
    public GameException(int code) {
        super(null, 200, code);
    }
    
    public GameException(int code, String message) {
        super(message, 200, code);
    }
    
    public static void error(int code) {
        throw new GameException(code);
    }
    
    public static void error(int code, String message) {
        throw new GameException(code, message);
    }
}
