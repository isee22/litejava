package example.controller;

import example.Services;
import example.model.Book;
import litejava.Context;
import litejava.Routes;

import java.util.List;
import java.util.Map;

/**
 * 图书控制器
 */
public class BookController {
    
    public Routes routes() {
        return new Routes()
            .get("/api/books", this::list)
            .get("/api/books/:id", this::get)
            .post("/api/books", this::create)
            .put("/api/books/:id", this::update)
            .delete("/api/books/:id", this::delete)
            .end();
    }
    
    void list(Context ctx) {
        String keyword = ctx.queryParam("q");
        List<Book> books = Services.book.search(keyword);
        ctx.ok(Map.of("books", books, "total", books.size()));
    }
    
    void get(Context ctx) {
        long id = ctx.pathParamLong("id");
        Book book = Services.book.getById(id);
        if (book != null) {
            ctx.ok(book);
        } else {
            ctx.fail(404, -1, "图书不存在");
        }
    }
    
    void create(Context ctx) {
        Book book = ctx.bindJSON(Book.class);
        
        if (book.title == null || book.title.isEmpty()) {
            ctx.fail(400, -1, "书名不能为空");
            return;
        }
        
        Book created = Services.book.create(book);
        ctx.status(201).ok(created);
    }
    
    void update(Context ctx) {
        long id = ctx.pathParamLong("id");
        Book book = ctx.bindJSON(Book.class);
        book.id = id;
        
        Book updated = Services.book.update(book);
        if (updated != null) {
            ctx.ok(updated);
        } else {
            ctx.fail(404, -1, "图书不存在");
        }
    }
    
    void delete(Context ctx) {
        long id = ctx.pathParamLong("id");
        if (Services.book.delete(id)) {
            ctx.ok("删除成功");
        } else {
            ctx.fail(404, -1, "图书不存在");
        }
    }
}
