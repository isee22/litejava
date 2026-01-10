package productservice.mapper;

import org.apache.ibatis.annotations.*;
import productservice.model.Category;

import java.util.List;

public interface CategoryMapper {
    
    @Select("SELECT * FROM categories WHERE id = #{id}")
    Category findById(Long id);
    
    @Select("SELECT * FROM categories ORDER BY sort_order")
    List<Category> findAll();
    
    @Select("SELECT * FROM categories WHERE parent_id = #{parentId} ORDER BY sort_order")
    List<Category> findByParentId(Long parentId);
    
    @Insert("INSERT INTO categories (name, parent_id, sort_order) VALUES (#{name}, #{parentId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Category category);
    
    @Update("UPDATE categories SET name=#{name}, parent_id=#{parentId}, sort_order=#{sortOrder} WHERE id=#{id}")
    int update(Category category);
    
    @Delete("DELETE FROM categories WHERE id = #{id}")
    int delete(Long id);
}
