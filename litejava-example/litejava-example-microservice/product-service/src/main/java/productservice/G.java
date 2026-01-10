package productservice;

import litejava.App;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import productservice.mapper.CategoryMapper;
import productservice.mapper.ProductMapper;

/**
 * 全局业务组件
 */
public class G {
    
    public static App app;
    
    public static ProductMapper productMapper;
    public static CategoryMapper categoryMapper;
    
    public static void init() {
        MyBatisPlugin mybatis = app.getPlugin(MyBatisPlugin.class);
        productMapper = mybatis.getMapper(ProductMapper.class);
        categoryMapper = mybatis.getMapper(CategoryMapper.class);
    }
    
    public static ConsulPlugin consul() {
        return app.getPlugin(ConsulPlugin.class);
    }
}
