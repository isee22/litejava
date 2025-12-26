package example;

import example.dao.BookDao;
import example.dao.SessionDao;
import example.dao.UserDao;

/**
 * DAO 实例集合
 */
public class Dao {
    public static final UserDao user = new UserDao();
    public static final SessionDao session = new SessionDao();
    public static final BookDao book = new BookDao();
}
