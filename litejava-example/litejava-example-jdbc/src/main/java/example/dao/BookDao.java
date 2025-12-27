package example.dao;

import example.model.Book;
import litejava.plugins.cache.CachePlugin;
import litejava.plugins.database.JdbcPlugin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

/**
 * 图书数据访问 - 使用原生 JdbcTemplate
 */
public class BookDao {
    
    private JdbcTemplate jdbc() {
        return JdbcPlugin.instance.jdbcTemplate;
    }
    
    private static final RowMapper<Book> ROW_MAPPER = (rs, rowNum) -> {
        Book book = new Book();
        book.id = rs.getLong("id");
        book.title = rs.getString("title");
        book.author = rs.getString("author");
        book.isbn = rs.getString("isbn");
        book.description = rs.getString("description");
        book.coverImage = rs.getString("cover_image");
        book.createdAt = rs.getTimestamp("created_at");
        book.updatedAt = rs.getTimestamp("updated_at");
        return book;
    };
    
    public Book findById(long id) {
        return CachePlugin.instance.getOrLoad("book:" + id, () -> {
            List<Book> list = jdbc().query(
                "SELECT * FROM books WHERE id = ?", ROW_MAPPER, id);
            return list.isEmpty() ? null : list.get(0);
        });
    }
    
    public List<Book> findAll() {
        return CachePlugin.instance.getOrLoad("book:all", () -> 
            jdbc().query("SELECT * FROM books ORDER BY id DESC", ROW_MAPPER));
    }
    
    public List<Book> search(String keyword) {
        String pattern = "%" + keyword + "%";
        return jdbc().query(
            "SELECT * FROM books WHERE title LIKE ? OR author LIKE ?",
            ROW_MAPPER, pattern, pattern);
    }
    
    public void create(Book book) {
        jdbc().update(
            "INSERT INTO books (title, author, isbn, description, cover_image) VALUES (?, ?, ?, ?, ?)",
            book.title, book.author, book.isbn, book.description, book.coverImage);
        CachePlugin.instance.del("book:all");
    }
    
    public int update(Book book) {
        int rows = jdbc().update(
            "UPDATE books SET title = ?, author = ?, isbn = ?, description = ?, cover_image = ? WHERE id = ?",
            book.title, book.author, book.isbn, book.description, book.coverImage, book.id);
        CachePlugin.instance.del("book:" + book.id);
        CachePlugin.instance.del("book:all");
        return rows;
    }
    
    public int delete(long id) {
        int rows = jdbc().update("DELETE FROM books WHERE id = ?", id);
        CachePlugin.instance.del("book:" + id);
        CachePlugin.instance.del("book:all");
        return rows;
    }
}
