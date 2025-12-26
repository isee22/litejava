package example.mapper;

import example.model.Book;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 图书 Mapper
 * 
 * 字段映射由 MyBatis 自动处理 (mapUnderscoreToCamelCase=true)
 * cover_image → coverImage
 */
public interface BookMapper {
    
    @Select("SELECT * FROM books WHERE id = #{id}")
    Book findById(long id);
    
    @Select("SELECT * FROM books")
    List<Book> findAll();
    
    @Select("SELECT * FROM books WHERE title LIKE #{keyword} OR author LIKE #{keyword}")
    List<Book> search(@Param("keyword") String keyword);
    
    @Insert("INSERT INTO books (title, author, isbn, description, cover_image, created_at, updated_at) " +
            "VALUES (#{title}, #{author}, #{isbn}, #{description}, #{coverImage}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Book book);
    
    @Update("UPDATE books SET title=#{title}, author=#{author}, isbn=#{isbn}, " +
            "description=#{description}, cover_image=#{coverImage}, updated_at=#{updatedAt} WHERE id=#{id}")
    void update(Book book);
    
    @Delete("DELETE FROM books WHERE id = #{id}")
    void delete(long id);
}
