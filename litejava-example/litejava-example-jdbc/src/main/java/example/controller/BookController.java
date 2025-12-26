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
                .summary("获取图书列表")
                .desc("支持关键字搜索")
                .tags("图书管理")
                .param("q", String.class, false, "搜索关键字")
                .response(200, Map.class, "图书列表")
            .get("/api/books/:id", this::get)
                .summary("获取图书详情")
                .tags("图书管理")
                .param("id", Long.class, "图书ID")
                .response(200, Book.class, "图书详情")
                .response(404, Map.class, "图书不存在")
            .post("/api/books", this::create)
                .summary("创建图书")
                .tags("图书管理")
                .body(Book.class, "图书信息")
                .response(201, Book.class, "创建成功")
                .response(400, Map.class, "参数错误")
            .put("/api/books/:id", this::update)
                .summary("更新图书")
                .tags("图书管理")
                .param("id", Long.class, "图书ID")
                .body(Book.class, "图书信息")
                .response(200, Book.class, "更新成功")
                .response(404, Map.class, "图书不存在")
            .delete("/api/books/:id", this::delete)
                .summary("删除图书")
                .tags("图书管理")
                .param("id", Long.class, "图书ID")
                .response(200, Map.class, "删除成功")
                .response(404, Map.class, "图书不存在")
            .end();
    }
    
    void list(Context ctx) {
        String keyword = ctx.queryParam("q");
        List<Book> books = Services.book.search(keyword);
        ctx.ok(Map.of("books", books, "total", books.size()));
    }
    
    void get(Context ctx) {
        long id = ctx.pathParam("id", Long.class);
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
        long id = ctx.pathParam("id", Long.class);
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
        long id = ctx.pathParam("id", Long.class);
        if (Services.book.delete(id)) {
            ctx.ok("删除成功");
        } else {
            ctx.fail(404, -1, "图书不存在");
        }
    }
}
