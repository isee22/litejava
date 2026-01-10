package productservice.service;

import productservice.G;
import productservice.model.Category;
import productservice.model.Product;

import java.util.List;

/**
 * 商品服务
 */
public class ProductService {
    
    public static List<Product> findAll() {
        return G.productMapper.findAll();
    }
    
    public static Product findById(Long id) {
        Product product = G.productMapper.findById(id);
        if (product != null && product.categoryId != null) {
            product.category = G.categoryMapper.findById(product.categoryId);
        }
        return product;
    }
    
    public static List<Product> findByCategoryId(Long categoryId) {
        return G.productMapper.findByCategoryId(categoryId);
    }
    
    public static List<Product> search(String keyword) {
        return G.productMapper.search(keyword);
    }
    
    public static Product create(Product product) {
        G.productMapper.insert(product);
        return product;
    }
    
    public static Product update(Long id, Product product) {
        product.id = id;
        if (G.productMapper.update(product) > 0) {
            return G.productMapper.findById(id);
        }
        return null;
    }
    
    public static boolean decreaseStock(Long id, Integer quantity) {
        return G.productMapper.decreaseStock(id, quantity) > 0;
    }
    
    public static boolean increaseStock(Long id, Integer quantity) {
        return G.productMapper.increaseStock(id, quantity) > 0;
    }
    
    public static boolean delete(Long id) {
        return G.productMapper.delete(id) > 0;
    }
    
    public static List<Category> findAllCategories() {
        return G.categoryMapper.findAll();
    }
    
    public static Category createCategory(Category category) {
        G.categoryMapper.insert(category);
        return category;
    }
}
