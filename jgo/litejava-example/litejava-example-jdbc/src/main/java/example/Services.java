package example;

import example.service.AuthService;
import example.service.BookService;
import example.service.FileService;

/**
 * Service 注册表 - 所有 Service 单例
 */
public class Services {
    public static final AuthService auth = new AuthService();
    public static final BookService book = new BookService();
    public static FileService file;
    
    public static void initFileService(String uploadDir, long maxSize) {
        file = new FileService(uploadDir, maxSize);
    }
}
