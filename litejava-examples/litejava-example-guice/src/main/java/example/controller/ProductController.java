package example.controller;

import example.service.ProductService;
import litejava.Context;

import javax.inject.Inject;
import java.util.Map;

/**
 * 产品控制器 - 演示注入无接口的具体类
 * 
 * <p>ProductService 没有接口，直接注入具体类也完全可以。
 * 适用于简单场景，不需要多态或 mock 测试时。
 */
public class ProductController {
    
    private final ProductService productService;
    
    @Inject
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    public void list(Context ctx) {
        ctx.json(Map.of("list", productService.list()));
    }
    
    public void get(Context ctx) {
        long id = ctx.pathParamLong("id");
        Map<String, Object> product = productService.get(id);
        if (product == null) {
            ctx.status(404).json(Map.of("error", "Product not found"));
            return;
        }
        ctx.json(product);
    }
    
    public void create(Context ctx) {
        Map<String, Object> data = ctx.bindJSON();
        Map<String, Object> product = productService.create(data);
        ctx.status(201).json(product);
    }
}
