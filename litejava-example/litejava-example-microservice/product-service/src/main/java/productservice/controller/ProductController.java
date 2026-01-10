package productservice.controller;

import common.BizException;
import common.Err;
import common.vo.ListResult;
import litejava.Context;
import litejava.Routes;
import productservice.model.Category;
import productservice.model.Product;
import productservice.service.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProductController {
    
    public static Routes routes() {
        return new Routes()
            .post("/product/list", ProductController::list)
            .post("/product/search", ProductController::search)
            .post("/product/detail", ProductController::detail)
            .post("/product/create", ProductController::create)
            .post("/product/update", ProductController::update)
            .post("/product/delete", ProductController::delete)
            .post("/product/stock/decrease", ProductController::decreaseStock)
            .post("/product/stock/increase", ProductController::increaseStock)
            .post("/category/list", ProductController::listCategories)
            .post("/category/products", ProductController::listByCategory)
            .post("/category/create", ProductController::createCategory)
            .end();
    }
    
    static void list(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        Long categoryId = body.get("categoryId") != null ? ((Number) body.get("categoryId")).longValue() : null;
        
        List<Product> products;
        if (categoryId != null) {
            products = ProductService.findByCategoryId(categoryId);
        } else {
            products = ProductService.findAll();
        }
        ctx.ok(ListResult.of(products));
    }
    
    static void search(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String keyword = (String) body.get("keyword");
        if (keyword == null || keyword.isEmpty()) BizException.paramRequired("keyword");
        
        List<Product> products = ProductService.search(keyword);
        ctx.ok(ListResult.of(products));
    }
    
    static void detail(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null) BizException.paramRequired("id");
        
        Long id = ((Number) body.get("id")).longValue();
        Product product = ProductService.findById(id);
        if (product == null) BizException.error(Err.PRODUCT_NOT_FOUND, "商品不存在");
        ctx.ok(product);
    }
    
    static void create(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String name = (String) body.get("name");
        if (name == null || name.isEmpty()) BizException.paramRequired("商品名称");
        if (body.get("price") == null || body.get("stock") == null) BizException.paramInvalid("价格和库存不能为空");
        
        Product product = new Product();
        product.name = name;
        product.categoryId = body.get("categoryId") != null ? ((Number) body.get("categoryId")).longValue() : null;
        product.price = new BigDecimal(body.get("price").toString());
        product.stock = ((Number) body.get("stock")).intValue();
        product.description = (String) body.get("description");
        product.imageUrl = (String) body.get("imageUrl");
        Product created = ProductService.create(product);
        ctx.ok(created);
    }
    
    static void update(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null) BizException.paramRequired("id");
        
        Long id = ((Number) body.get("id")).longValue();
        Product product = new Product();
        product.name = (String) body.get("name");
        product.categoryId = body.get("categoryId") != null ? ((Number) body.get("categoryId")).longValue() : null;
        product.price = body.get("price") != null ? new BigDecimal(body.get("price").toString()) : null;
        product.stock = body.get("stock") != null ? ((Number) body.get("stock")).intValue() : null;
        product.description = (String) body.get("description");
        product.imageUrl = (String) body.get("imageUrl");
        Product updated = ProductService.update(id, product);
        if (updated == null) BizException.error(Err.PRODUCT_NOT_FOUND, "商品不存在");
        ctx.ok(updated);
    }
    
    static void delete(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null) BizException.paramRequired("id");
        
        Long id = ((Number) body.get("id")).longValue();
        if (!ProductService.delete(id)) BizException.error(Err.PRODUCT_NOT_FOUND, "商品不存在");
        ctx.ok();
    }
    
    static void decreaseStock(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null || body.get("quantity") == null) BizException.paramInvalid("id, quantity 不能为空");
        
        Long id = ((Number) body.get("id")).longValue();
        Integer quantity = ((Number) body.get("quantity")).intValue();
        if (!ProductService.decreaseStock(id, quantity)) BizException.error(Err.STOCK_NOT_ENOUGH, "库存不足或商品不存在");
        ctx.ok();
    }
    
    static void increaseStock(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null || body.get("quantity") == null) BizException.paramInvalid("id, quantity 不能为空");
        
        Long id = ((Number) body.get("id")).longValue();
        Integer quantity = ((Number) body.get("quantity")).intValue();
        if (!ProductService.increaseStock(id, quantity)) BizException.error(Err.PRODUCT_NOT_FOUND, "商品不存在");
        ctx.ok();
    }
    
    static void listCategories(Context ctx) {
        List<Category> categories = ProductService.findAllCategories();
        ctx.ok(ListResult.of(categories));
    }
    
    static void listByCategory(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("categoryId") == null) BizException.paramRequired("categoryId");
        
        Long categoryId = ((Number) body.get("categoryId")).longValue();
        List<Product> products = ProductService.findByCategoryId(categoryId);
        ctx.ok(ListResult.of(products));
    }
    
    static void createCategory(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String name = (String) body.get("name");
        if (name == null || name.isEmpty()) BizException.paramRequired("分类名称");
        
        Category category = new Category();
        category.name = name;
        category.parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : 0L;
        category.sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : 0;
        Category created = ProductService.createCategory(category);
        ctx.ok(created);
    }
}
