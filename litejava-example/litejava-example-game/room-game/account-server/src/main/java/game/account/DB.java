package game.account;

import litejava.plugins.database.MyBatisPlugin;

import java.util.function.Function;

/**
 * 数据库访问
 */
public class DB {
    
    public static <T, R> R execute(Class<T> mapperClass, Function<T, R> action) {
        return MyBatisPlugin.instance.execute(mapperClass, action);
    }
}
