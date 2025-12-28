package example;

import example.service.BookService;
import litejava.plugins.database.MyBatisPlugin;

/**
 * Service 实例集合
 */
public class Services {
    public static BookService book;
    
    public static void init(MyBatisPlugin mybatis) {
        book = new BookService(mybatis);
    }
}
