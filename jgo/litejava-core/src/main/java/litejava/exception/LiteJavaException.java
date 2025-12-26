package litejava.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all LiteJava framework exceptions.
 */
public class LiteJavaException extends RuntimeException {
    
    public int statusCode = 500;
    public Map<String, Object> details = new HashMap<>();
    
    public LiteJavaException(String message) {
        super(message);
    }
    
    public LiteJavaException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public LiteJavaException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public LiteJavaException(String message, int statusCode, Map<String, Object> details) {
        super(message);
        this.statusCode = statusCode;
        if (details != null) {
            this.details = details;
        }
    }
}
