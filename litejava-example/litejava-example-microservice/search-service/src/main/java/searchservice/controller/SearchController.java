package searchservice.controller;

import common.BizException;
import common.Err;
import litejava.Context;
import litejava.Routes;
import searchservice.service.SearchService;

import java.util.List;
import java.util.Map;

public class SearchController {
    
    public static Routes routes() {
        return new Routes()
            .post("/search/query", SearchController::search)
            .post("/search/suggest", SearchController::suggest)
            .post("/search/hot", SearchController::hot)
            .post("/search/sync", SearchController::sync)
            .post("/search/recommend", SearchController::recommend)
            .end();
    }
    
    static void search(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String keyword = (String) body.get("keyword");
        if (keyword == null || keyword.isEmpty()) BizException.paramRequired("keyword");
        
        String userIdStr = ctx.header("X-User-Id");
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 1;
        int size = body.get("size") != null ? ((Number) body.get("size")).intValue() : 10;
        
        Map<String, Object> result = SearchService.search(keyword, page, size, userId);
        ctx.ok(result);
    }
    
    static void suggest(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String prefix = (String) body.get("prefix");
        if (prefix == null || prefix.isEmpty()) BizException.paramRequired("prefix");
        
        int limit = body.get("limit") != null ? ((Number) body.get("limit")).intValue() : 10;
        List<String> suggestions = SearchService.suggest(prefix, limit);
        ctx.ok(suggestions);
    }
    
    static void hot(Context ctx) {
        String userIdStr = ctx.header("X-User-Id");
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;
        List<String> keywords = SearchService.hotKeywords(userId);
        ctx.ok(keywords);
    }
    
    static void sync(Context ctx) {
        int count = SearchService.syncIndexManual();
        Map<String, Object> result = Map.of("indexed", count);
        ctx.ok(result);
    }
    
    static void recommend(Context ctx) {
        String userIdStr = ctx.header("X-User-Id");
        if (userIdStr == null) BizException.error(Err.UNAUTHORIZED, "请先登录");
        
        Long userId = Long.parseLong(userIdStr);
        Map<String, Object> body = ctx.bindJSON();
        int size = body.get("size") != null ? ((Number) body.get("size")).intValue() : 10;
        
        List<Map<String, Object>> recommendations = SearchService.recommend(userId, size);
        ctx.ok(recommendations);
    }
}
