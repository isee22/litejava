package searchservice.service;

import searchservice.G;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务 - 支持千人千面个性化搜索
 */
public class SearchService {
    
    private static List<Map<String, Object>> productIndex = new ArrayList<>();
    private static long lastSyncTime = 0;
    private static final long SYNC_INTERVAL = 60000;
    
    // 用户搜索历史（实际应存 Redis）
    private static Map<Long, List<String>> userSearchHistory = new HashMap<>();
    // 用户偏好分类（实际应存 Redis）
    private static Map<Long, Map<Long, Integer>> userCategoryPreference = new HashMap<>();
    
    /**
     * 个性化搜索
     */
    public static Map<String, Object> search(String keyword, int page, int size, Long userId) {
        syncIndex();
        
        // 记录用户搜索历史
        if (userId != null) {
            recordSearchHistory(userId, keyword);
        }
        
        String lowerKeyword = keyword.toLowerCase();
        
        List<Map<String, Object>> matched = productIndex.stream()
            .filter(p -> {
                String name = ((String) p.get("name")).toLowerCase();
                String desc = p.get("description") != null ? 
                    ((String) p.get("description")).toLowerCase() : "";
                return name.contains(lowerKeyword) || desc.contains(lowerKeyword);
            })
            .collect(Collectors.toList());
        
        // 千人千面：根据用户偏好排序
        if (userId != null) {
            matched = personalizeResults(matched, userId);
        }
        
        int total = matched.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        
        List<Map<String, Object>> results = fromIndex < total ? 
            new ArrayList<>(matched.subList(fromIndex, toIndex)) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", results);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
    
    /**
     * 根据用户偏好对结果排序
     */
    private static List<Map<String, Object>> personalizeResults(List<Map<String, Object>> products, Long userId) {
        Map<Long, Integer> preferences = userCategoryPreference.get(userId);
        if (preferences == null || preferences.isEmpty()) {
            return products;
        }
        
        // 按用户偏好分类权重排序
        return products.stream()
            .sorted((a, b) -> {
                Long catA = a.get("categoryId") != null ? ((Number) a.get("categoryId")).longValue() : 0L;
                Long catB = b.get("categoryId") != null ? ((Number) b.get("categoryId")).longValue() : 0L;
                int weightA = preferences.getOrDefault(catA, 0);
                int weightB = preferences.getOrDefault(catB, 0);
                return weightB - weightA; // 权重高的排前面
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 记录搜索历史
     */
    private static void recordSearchHistory(Long userId, String keyword) {
        List<String> history = userSearchHistory.computeIfAbsent(userId, k -> new ArrayList<>());
        history.remove(keyword); // 去重
        history.add(0, keyword); // 最新的放前面
        if (history.size() > 20) {
            history.remove(history.size() - 1);
        }
    }
    
    /**
     * 记录用户点击/购买行为，更新偏好
     */
    public static void recordUserBehavior(Long userId, Long categoryId) {
        Map<Long, Integer> preferences = userCategoryPreference.computeIfAbsent(userId, k -> new HashMap<>());
        preferences.merge(categoryId, 1, Integer::sum);
    }
    
    public static List<String> suggest(String prefix, int limit) {
        syncIndex();
        
        String lowerPrefix = prefix.toLowerCase();
        
        return productIndex.stream()
            .map(p -> (String) p.get("name"))
            .filter(name -> name.toLowerCase().startsWith(lowerPrefix))
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 个性化热搜
     */
    public static List<String> hotKeywords(Long userId) {
        List<String> defaultHot = Arrays.asList("iPhone", "MacBook", "AirPods", "iPad", "Apple Watch");
        
        if (userId == null) {
            return defaultHot;
        }
        
        // 结合用户搜索历史
        List<String> history = userSearchHistory.get(userId);
        if (history == null || history.isEmpty()) {
            return defaultHot;
        }
        
        // 用户历史 + 热门关键词
        List<String> result = new ArrayList<>();
        result.addAll(history.subList(0, Math.min(3, history.size())));
        for (String hot : defaultHot) {
            if (!result.contains(hot) && result.size() < 10) {
                result.add(hot);
            }
        }
        return result;
    }
    
    /**
     * 个性化推荐
     */
    public static List<Map<String, Object>> recommend(Long userId, int size) {
        syncIndex();
        
        Map<Long, Integer> preferences = userCategoryPreference.get(userId);
        
        if (preferences == null || preferences.isEmpty()) {
            // 无偏好数据，返回热门商品
            return productIndex.stream()
                .limit(size)
                .collect(Collectors.toList());
        }
        
        // 找出用户最喜欢的分类
        Long favoriteCategory = preferences.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        // 优先推荐该分类商品
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 先加偏好分类的商品
        productIndex.stream()
            .filter(p -> {
                Long catId = p.get("categoryId") != null ? ((Number) p.get("categoryId")).longValue() : 0L;
                return catId.equals(favoriteCategory);
            })
            .limit(size / 2)
            .forEach(result::add);
        
        // 再加其他商品
        productIndex.stream()
            .filter(p -> !result.contains(p))
            .limit(size - result.size())
            .forEach(result::add);
        
        return result;
    }
    
    public static int syncIndexManual() {
        lastSyncTime = 0;
        syncIndex();
        return productIndex.size();
    }
    
    private static void syncIndex() {
        long now = System.currentTimeMillis();
        
        if (now - lastSyncTime < SYNC_INTERVAL && !productIndex.isEmpty()) {
            return;
        }
        
        List<Map<String, Object>> products = ProductClient.getAllProducts();
        if (!products.isEmpty()) {
            productIndex = new ArrayList<>(products);
            lastSyncTime = now;
        }
    }
}
