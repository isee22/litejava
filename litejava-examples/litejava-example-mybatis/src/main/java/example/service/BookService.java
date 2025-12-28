package example.service;

import example.mapper.BookMapper;
import example.model.Book;
import litejava.plugins.database.MyBatisPlugin;

import java.sql.Timestamp;
import java.util.List;

/**
 * 图书服务 - 直接使用 Mapper
 */
public class BookService {
    
    public Book create(Book book) {
        book.createdAt = new Timestamp(System.currentTimeMillis());
        book.updatedAt = book.createdAt;
        MyBatisPlugin.instance.tx(session -> {
            session.getMapper(BookMapper.class).insert(book);
            return null;
        });
        return book;
    }
    
    public Book getById(long id) {
        return MyBatisPlugin.instance.execute(BookMapper.class, m -> m.findById(id));
    }
    
    public List<Book> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return MyBatisPlugin.instance.execute(BookMapper.class, BookMapper::findAll);
        }
        return MyBatisPlugin.instance.execute(BookMapper.class, m -> m.search("%" + keyword + "%"));
    }
    
    public Book update(Book book) {
        book.updatedAt = new Timestamp(System.currentTimeMillis());
        MyBatisPlugin.instance.tx(session -> {
            session.getMapper(BookMapper.class).update(book);
            return null;
        });
        return book;
    }
    
    public boolean delete(long id) {
        MyBatisPlugin.instance.tx(session -> {
            session.getMapper(BookMapper.class).delete(id);
            return null;
        });
        return true;
    }
}
