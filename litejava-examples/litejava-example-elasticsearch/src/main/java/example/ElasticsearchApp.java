package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.search.ElasticsearchPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Elasticsearch 全文搜索示例
 * 
 * <h2>前置条件</h2>
 * 启动 ES: docker run -d -p 9200:9200 -e "discovery.type=single-node" elasticsearch:8.11.3
 * 
 * <h2>测试</h2>
 * <pre>
 * # 索引文档
 * curl -X POST http://localhost:8080/index -H "Content-Type: application/json" \
 *   -d '{"title":"Hello World","content":"This is a test document"}'
 * 
 * # 搜索
 * curl "http://localhost:8080/search?q=hello"
 * </pre>
 */
public class ElasticsearchApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        ElasticsearchPlugin es = new ElasticsearchPlugin();
        es.host = "localhost";
        es.port = 9200;
        app.use(es);
        
        // 索引文档
        app.post("/index", ctx -> {
            Map<String, Object> doc = ctx.bindJSON();
            String id = UUID.randomUUID().toString();
            es.index("articles", id, doc);
            ctx.json(Map.of("id", id, "indexed", true));
        });
        
        // 搜索
        app.get("/search", ctx -> {
            String query = ctx.queryParam("q");
            List<Map> results = es.searchMultiMatch("articles", query, "title", "content");
            ctx.json(Map.of("results", results));
        });
        
        // 获取文档
        app.get("/doc/:id", ctx -> {
            String id = ctx.pathParam("id");
            Map<String, Object> doc = es.get("articles", id);
            if (doc == null) {
                ctx.status(404).json(Map.of("error", "Not found"));
                return;
            }
            ctx.json(doc);
        });
        
        // 删除文档
        app.delete("/doc/:id", ctx -> {
            String id = ctx.pathParam("id");
            es.delete("articles", id);
            ctx.json(Map.of("deleted", true));
        });
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "Elasticsearch Example",
            "endpoints", Map.of(
                "POST /index", "Index a document",
                "GET /search?q=", "Search documents",
                "GET /doc/:id", "Get document by ID",
                "DELETE /doc/:id", "Delete document"
            )
        )));
        
        app.run();
    }
}
