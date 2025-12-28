package example;

import example.dao.BookDao;
import litejava.plugins.database.JpaPlugin;

/**
 * DAO 实例集合
 */
public class Dao {
    public static BookDao book;
    
    public static void init(JpaPlugin jpa) {
        book = new BookDao(jpa);
    }
}
