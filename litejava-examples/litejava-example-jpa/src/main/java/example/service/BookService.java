package example.service;

import example.Dao;
import example.model.Book;

import java.util.List;

/**
 * 图书服务
 */
public class BookService {
    
    public Book create(Book book) {
        Dao.book.create(book);
        return book;
    }
    
    public Book getById(long id) {
        return Dao.book.findById(id);
    }
    
    public List<Book> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return Dao.book.findAll();
        }
        return Dao.book.search(keyword);
    }
    
    public Book update(Book book) {
        Dao.book.update(book);
        return book;
    }
    
    public boolean delete(long id) {
        Dao.book.delete(id);
        return true;
    }
}
