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
    
    private final MyBatisPlugin mybatis;
    
    public BookService(MyBatisPlugin mybatis) {
        this.mybatis = mybatis;
    }
    
    public Book create(Book book) {
        book.createdAt = new Timestamp(System.currentTimeMillis());
        book.updatedAt = book.createdAt;
        mybatis.tx(session -> {
            session.getMapper(BookMapper.class).insert(book);
            return null;
        });
        return book;
    }
    
    public Book getById(long id) {
        return mybatis.execute(BookMapper.class, m -> m.findById(id));
    }
    
    public List<Book> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return mybatis.execute(BookMapper.class, BookMapper::findAll);
        }
        return mybatis.execute(BookMapper.class, m -> m.search("%" + keyword + "%"));
    }
    
    public Book update(Book book) {
        book.updatedAt = new Timestamp(System.currentTimeMillis());
        mybatis.tx(session -> {
            session.getMapper(BookMapper.class).update(book);
            return null;
        });
        return book;
    }
    
    public boolean delete(long id) {
        mybatis.tx(session -> {
            session.getMapper(BookMapper.class).delete(id);
            return null;
        });
        return true;
    }
}
