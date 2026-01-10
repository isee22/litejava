package productservice.mapper;

import org.apache.ibatis.annotations.*;
import productservice.model.Product;

import java.util.List;

public interface ProductMapper {
    
    @Select("SELECT * FROM products WHERE id = #{id} AND status = 1")
    Product findById(Long id);
    
    @Select("SELECT * FROM products WHERE status = 1 ORDER BY created_at DESC")
    List<Product> findAll();
    
    @Select("SELECT * FROM products WHERE category_id = #{categoryId} AND status = 1")
    List<Product> findByCategoryId(Long categoryId);
    
    @Select("SELECT * FROM products WHERE name LIKE CONCAT('%', #{keyword}, '%') AND status = 1")
    List<Product> search(String keyword);
    
    @Insert("INSERT INTO products (name, category_id, price, stock, description, image_url, status) " +
            "VALUES (#{name}, #{categoryId}, #{price}, #{stock}, #{description}, #{imageUrl}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Product product);
    
    @Update("UPDATE products SET name=#{name}, category_id=#{categoryId}, price=#{price}, " +
            "stock=#{stock}, description=#{description}, image_url=#{imageUrl}, updated_at=NOW() WHERE id=#{id}")
    int update(Product product);
    
    @Update("UPDATE products SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{id}")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    @Update("UPDATE products SET status = 0 WHERE id = #{id}")
    int delete(Long id);
}
