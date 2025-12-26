package litejava.plugins.router;

import litejava.Handler;
import litejava.Route;
import litejava.plugin.RouterPlugin;

import java.util.*;
import java.util.function.Consumer;

/**
 * 标准 Trie 树路由插件
 * 
 * <p>适用场景：
 * <ul>
 *   <li>中等规模路由（50-500）</li>
 *   <li>需要参数和通配符支持</li>
 *   <li>实现简单，易于理解和调试</li>
 * </ul>
 * 
 * <p>特点：
 * <ul>
 *   <li>O(k) 匹配，k 为路径段数</li>
 *   <li>支持路径参数 :id</li>
 *   <li>支持通配符 *filepath</li>
 *   <li>比 Radix Tree 简单，但内存占用稍高</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new TrieRouterPlugin());
 * app.get("/users/:id", ctx -> ctx.json(user));
 * app.get("/files/*filepath", ctx -> ctx.file(path));
 * }</pre>
 * 
 * <h2>与 RadixTree 的区别</h2>
 * <ul>
 *   <li>Trie：每个路径段一个节点，如 /users/list 有 3 个节点</li>
 *   <li>RadixTree：压缩公共前缀，/users/list 可能只有 1-2 个节点</li>
 *   <li>RadixTree 内存更省，匹配更快，但实现更复杂</li>
 * </ul>
 */
public class TrieRouterPlugin extends RouterPlugin {
    
    private final TrieNode root = new TrieNode();
    private final List<Route> routeList = new ArrayList<>();
    private TrieRouterPlugin parent;
    
    public TrieRouterPlugin() {}
    
    protected TrieRouterPlugin(String prefix, TrieRouterPlugin parent) {
        this.prefix = prefix;
        this.parent = parent;
    }
    
    @Override
    public Route route(String method, String path, Handler handler) {
        String fullPath = normalizePath(prefix + path);
        
        TrieRouterPlugin root = getTrieRoot();
        root.root.insert(fullPath, method, handler);
        
        Route route = new Route(method, fullPath, handler);
        root.routeList.add(route);
        
        return route;
    }
    
    @Override
    public RouteMatch match(String method, String path) {
        Map<String, String> params = new HashMap<>();
        String[] wildcardHolder = new String[2];
        
        Handler handler = root.search(path, method, params, wildcardHolder);
        if (handler == null) return null;
        
        RouteMatch match = new RouteMatch();
        match.handler = handler;
        match.params = params.isEmpty() ? null : params;
        if (wildcardHolder[0] != null) {
            match.wildcardName = wildcardHolder[0];
            match.wildcardValue = wildcardHolder[1];
        }
        return match;
    }
    
    @Override
    public boolean hasPath(String path) {
        return root.hasPath(path);
    }
    
    @Override
    public Set<String> getAllowedMethods(String path) {
        return root.getAllowedMethods(path);
    }
    
    @Override
    public List<Route> getAllRoutes() {
        return new ArrayList<>(routeList);
    }
    
    @Override
    public RouterPlugin group(String groupPrefix) {
        return new TrieRouterPlugin(normalizePath(prefix + groupPrefix), getTrieRoot());
    }
    
    @Override
    public RouterPlugin group(String groupPrefix, Consumer<RouterPlugin> configure) {
        TrieRouterPlugin subRouter = new TrieRouterPlugin(normalizePath(prefix + groupPrefix), getTrieRoot());
        configure.accept(subRouter);
        return this;
    }
    
    private TrieRouterPlugin getTrieRoot() {
        return parent != null ? parent.getTrieRoot() : this;
    }
    
    /**
     * Trie 节点
     */
    private static class TrieNode {
        Map<String, TrieNode> children = new HashMap<>();
        TrieNode paramChild;
        String paramName;
        TrieNode wildcardChild;
        String wildcardName;
        Map<String, Handler> handlers;
        
        void insert(String path, String method, Handler handler) {
            String[] segments = path.split("/");
            TrieNode current = this;
            
            for (String segment : segments) {
                if (segment.isEmpty()) continue;
                
                if (segment.startsWith(":")) {
                    // 参数节点
                    if (current.paramChild == null) {
                        current.paramChild = new TrieNode();
                        current.paramName = segment.substring(1);
                    }
                    current = current.paramChild;
                } else if (segment.startsWith("*")) {
                    // 通配符节点
                    if (current.wildcardChild == null) {
                        current.wildcardChild = new TrieNode();
                        current.wildcardName = segment.length() > 1 ? segment.substring(1) : "wildcard";
                    }
                    current = current.wildcardChild;
                    break; // 通配符后面不再有路径
                } else {
                    // 静态节点
                    current = current.children.computeIfAbsent(segment, k -> new TrieNode());
                }
            }
            
            if (current.handlers == null) {
                current.handlers = new HashMap<>();
            }
            current.handlers.put(method, handler);
        }
        
        Handler search(String path, String method, Map<String, String> params, String[] wildcardHolder) {
            String[] segments = path.split("/");
            return searchRecursive(segments, 0, method, params, wildcardHolder);
        }
        
        private Handler searchRecursive(String[] segments, int index, String method, 
                                        Map<String, String> params, String[] wildcardHolder) {
            // 跳过空段
            while (index < segments.length && segments[index].isEmpty()) {
                index++;
            }
            
            // 到达末尾
            if (index >= segments.length) {
                return getHandler(method);
            }
            
            String segment = segments[index];
            
            // 1. 优先匹配静态路径
            TrieNode staticChild = children.get(segment);
            if (staticChild != null) {
                Handler h = staticChild.searchRecursive(segments, index + 1, method, params, wildcardHolder);
                if (h != null) return h;
            }
            
            // 2. 尝试参数匹配
            if (paramChild != null) {
                params.put(paramName, segment);
                Handler h = paramChild.searchRecursive(segments, index + 1, method, params, wildcardHolder);
                if (h != null) return h;
                params.remove(paramName);
            }
            
            // 3. 尝试通配符匹配
            if (wildcardChild != null) {
                StringBuilder remaining = new StringBuilder();
                for (int i = index; i < segments.length; i++) {
                    if (!segments[i].isEmpty()) {
                        if (remaining.length() > 0) remaining.append("/");
                        remaining.append(segments[i]);
                    }
                }
                wildcardHolder[0] = wildcardName;
                wildcardHolder[1] = remaining.toString();
                return wildcardChild.getHandler(method);
            }
            
            return null;
        }
        
        boolean hasPath(String path) {
            String[] segments = path.split("/");
            return hasPathRecursive(segments, 0);
        }
        
        private boolean hasPathRecursive(String[] segments, int index) {
            while (index < segments.length && segments[index].isEmpty()) {
                index++;
            }
            
            if (index >= segments.length) {
                return handlers != null;
            }
            
            String segment = segments[index];
            
            TrieNode staticChild = children.get(segment);
            if (staticChild != null && staticChild.hasPathRecursive(segments, index + 1)) {
                return true;
            }
            
            if (paramChild != null && paramChild.hasPathRecursive(segments, index + 1)) {
                return true;
            }
            
            return wildcardChild != null && wildcardChild.handlers != null;
        }
        
        Set<String> getAllowedMethods(String path) {
            Set<String> methods = new LinkedHashSet<>();
            String[] segments = path.split("/");
            collectMethods(segments, 0, methods);
            return methods;
        }
        
        private void collectMethods(String[] segments, int index, Set<String> methods) {
            while (index < segments.length && segments[index].isEmpty()) {
                index++;
            }
            
            if (index >= segments.length) {
                if (handlers != null) {
                    for (String m : handlers.keySet()) {
                        if ("ANY".equals(m)) {
                            methods.addAll(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"));
                        } else {
                            methods.add(m);
                        }
                    }
                }
                return;
            }
            
            String segment = segments[index];
            
            TrieNode staticChild = children.get(segment);
            if (staticChild != null) {
                staticChild.collectMethods(segments, index + 1, methods);
            }
            
            if (paramChild != null) {
                paramChild.collectMethods(segments, index + 1, methods);
            }
            
            if (wildcardChild != null && wildcardChild.handlers != null) {
                for (String m : wildcardChild.handlers.keySet()) {
                    if ("ANY".equals(m)) {
                        methods.addAll(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"));
                    } else {
                        methods.add(m);
                    }
                }
            }
        }
        
        private Handler getHandler(String method) {
            if (handlers == null) return null;
            Handler h = handlers.get(method);
            if (h == null) h = handlers.get("ANY");
            return h;
        }
    }
}
