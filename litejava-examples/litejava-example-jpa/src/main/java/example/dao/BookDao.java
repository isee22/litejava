package example.dao;

import litejava.plugins.database.JpaPlugin;
import example.model.Book;

import java.util.List;

/**
 * 图书数据访问 - 使用 JpaPlugin 辅助方法
 */
public class BookDao {
    
    public Book findById(long id) {
        return JpaPlugin.instance.query(em -> em.find(Book.class, id));
    }
    
    public List<Book> findAll() {
        return JpaPlugin.instance.query(em -> 
            em.createQuery("SELECT b FROM Book b", Book.class).getResultList());
    }
    
    public List<Book> search(String keyword) {
        return JpaPlugin.instance.query(em -> 
            em.createQuery("SELECT b FROM Book b WHERE b.title LIKE :kw OR b.author LIKE :kw", Book.class)
                .setParameter("kw", "%" + keyword + "%")
                .getResultList());
    }
    
    public void create(Book book) {
        JpaPlugin.instance.txVoid(em -> em.persist(book));
    }
    
    public void update(Book book) {
        JpaPlugin.instance.txVoid(em -> em.merge(book));
    }
    
    public void delete(long id) {
        JpaPlugin.instance.txVoid(em -> {
            Book book = em.find(Book.class, id);
            if (book != null) em.remove(book);
        });
    }
}
