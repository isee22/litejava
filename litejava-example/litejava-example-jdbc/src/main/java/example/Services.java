package example;

import example.service.AuthService;
import example.service.BookService;

/**
 * Service 注册表 - 所有 Service 单例
 * 
 * <p>文件处理功能已迁移到 FilePlugin，使用 app.file 访问。
 */
public class Services {
    public static final AuthService auth = new AuthService();
    public static final BookService book = new BookService();
}
