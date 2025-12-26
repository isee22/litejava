package example.dao;

import example.infra.Db;
import example.model.Book;
import litejava.plugins.cache.CachePlugin;

import java.util.List;

/**
 * 图书数据访问
 */
public class BookDao {
    
    public Book findById(long id) {
        return CachePlugin.instance.getOrLoad("book:" + id, () -> Db.first(Book.class, id));
    }
    
    public List<Book> findAll() {
        return CachePlugin.instance.getOrLoad("book:all", () -> Db.find(Book.class));
    }
    
    public List<Book> search(String keyword) {
        String pattern = "%" + keyword + "%";
        return Db.find(Book.class, "title LIKE ? OR author LIKE ?", pattern, pattern);
    }
    
    public void create(Book book) {
        Db.create(book);
        CachePlugin.instance.del("book:all");
    }
    
    public int update(Book book) {
        int rows = Db.update(book);
        CachePlugin.instance.del("book:" + book.id);
        CachePlugin.instance.del("book:all");
        return rows;
    }
    
    public int delete(long id) {
        int rows = Db.delete(Book.class, id);
        CachePlugin.instance.del("book:" + id);
        CachePlugin.instance.del("book:all");
        return rows;
    }
}
