package litejava.plugin;

import litejava.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * 路由插件 - 基于 Radix Tree 的高效路由
 * 
 * <p>LiteJava 的路由插件，借鉴 Gin 的设计：
 * <ul>
 *   <li>路径参数：{@code /users/:id}</li>
 *   <li>通配符路由：{@code /files/*filepath}</li>
 *   <li>路由分组：{@code router.group("/api")}</li>
 *   <li>ANY 方法匹配</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // App 默认使用 RouterPlugin
 * app.get("/users/:id", ctx -> ctx.json(user));
 * 
 * // 自定义路由（继承扩展）
 * class MyRouter extends RouterPlugin {
 *     @Override
 *     public RouteMatch match(String method, String path) {
 *         // 自定义匹配逻辑
 *     }
 * }
 * app.use(new MyRouter());
 * }</pre>
 */
public class RouterPlugin extends Plugin {
    
    /** 路由前缀（用于分组） */
    public String prefix = "";
    
    /** 父路由（用于分组） */
    private RouterPlugin parent;
    
    /** Radix Tree 路由匹配 */
    private final RadixTree tree = new RadixTree();
    
    /** 路由列表（用于调试） */
    private final List<Route> routes = new ArrayList<>();
    
    /** 404 处理器 */
    public Handler noRouteHandler;
    
    /** 405 处理器 */
    public Handler noMethodHandler;
    
    public RouterPlugin() {}
    
    protected RouterPlugin(String prefix, RouterPlugin parent) {
        this.prefix = prefix;
        this.parent = parent;
    }
    
    @Override
    public void config() {
        // 默认路由无需额外配置
    }

    // ==================== 路由注册 ====================
    
    public Route get(String path, Handler handler) {
        return route("GET", path, handler);
    }
    
    public Route post(String path, Handler handler) {
        return route("POST", path, handler);
    }
    
    public Route put(String path, Handler handler) {
        return route("PUT", path, handler);
    }
    
    public Route delete(String path, Handler handler) {
        return route("DELETE", path, handler);
    }
    
    public Route patch(String path, Handler handler) {
        return route("PATCH", path, handler);
    }
    
    public Route head(String path, Handler handler) {
        return route("HEAD", path, handler);
    }
    
    public Route options(String path, Handler handler) {
        return route("OPTIONS", path, handler);
    }
    
    public Route any(String path, Handler handler) {
        return route("ANY", path, handler);
    }
    
    public Route route(String method, String path, Handler handler) {
        String fullPath = normalizePath(prefix + path);
        
        // 添加到树（如果是子路由，添加到根路由的树）
        RouterPlugin root = getRoot();
        root.tree.add(fullPath, handler, method);
        
        Route route = new Route(method, fullPath, handler);
        root.routes.add(route);
        
        return route;
    }
    
    // ==================== 特殊处理器 ====================
    
    public RouterPlugin noRoute(Handler handler) {
        getRoot().noRouteHandler = handler;
        return this;
    }
    
    public RouterPlugin noMethod(Handler handler) {
        getRoot().noMethodHandler = handler;
        return this;
    }
    
    // ==================== 路由分组 ====================
    
    public RouterPlugin group(String groupPrefix, Consumer<RouterPlugin> configure) {
        RouterPlugin subRouter = createSubRouter(normalizePath(prefix + groupPrefix));
        configure.accept(subRouter);
        return this;
    }
    
    public RouterPlugin group(String groupPrefix) {
        return createSubRouter(normalizePath(prefix + groupPrefix));
    }
    
    /** 创建子路由（子类可重写） */
    protected RouterPlugin createSubRouter(String prefix) {
        return new RouterPlugin(prefix, getRoot());
    }
    
    // ==================== 路由匹配 ====================
    
    public RouteMatch match(String method, String path) {
        RadixTree.MatchResult result = tree.find(path, method);
        if (result == null) return null;
        
        RouteMatch match = new RouteMatch();
        match.handler = result.handler;
        match.params = result.params;
        match.wildcardName = result.wildcardName;
        match.wildcardValue = result.wildcardValue;
        return match;
    }
    
    public boolean hasPath(String path) {
        return tree.hasPath(path);
    }
    
    public Set<String> getAllowedMethods(String path) {
        return tree.getAllowedMethods(path);
    }
    
    // ==================== 调试 ====================
    
    public List<Route> getAllRoutes() {
        return new ArrayList<>(routes);
    }
    
    public void printRoutes() {
        printRoutes(System.out::println);
    }
    
    public void printRoutes(java.util.function.Consumer<String> printer) {
        printer.accept("Registered Routes:");
        for (Route route : routes) {
            printer.accept(String.format("  %-7s %s", route.method, route.path));
        }
    }
    
    // ==================== 批量注册 ====================
    
    public RouterPlugin register(Routes routes) {
        // 优先使用新的 routes 列表（包含元数据）
        if (!routes.routes.isEmpty()) {
            for (Route r : routes.routes) {
                String fullPath = normalizePath(prefix + r.path);
                RouterPlugin root = getRoot();
                root.tree.add(fullPath, r.handler, r.method);
                
                // 复制路由并更新路径
                Route newRoute = new Route(r.method, fullPath, r.handler);
                newRoute.summary = r.summary;
                newRoute.description = r.description;
                newRoute.tags = r.tags;
                newRoute.params = r.params;
                newRoute.responses = r.responses;
                newRoute.requestBody = r.requestBody;
                newRoute.requestBodyDesc = r.requestBodyDesc;
                root.routes.add(newRoute);
            }
        } else {
            // 兼容旧代码
            for (Routes.Entry entry : routes.entries) {
                route(entry.method, entry.path, entry.handler);
            }
        }
        return this;
    }
    
    // ==================== 内部方法 ====================
    
    protected RouterPlugin getRoot() {
        return parent != null ? parent.getRoot() : this;
    }
    
    protected String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        while (path.contains("//")) path = path.replace("//", "/");
        return path;
    }
    
    /**
     * 路由匹配结果
     */
    public static class RouteMatch {
        public Handler handler;
        public Map<String, String> params;
        public String wildcardName;
        public String wildcardValue;
    }
    
    // ==================== Radix Tree 实现 ====================
    
    /**
     * Radix Tree (压缩前缀树) - Gin 风格高性能路由
     * 时间复杂度: O(k)，k 为路径长度
     */
    private static class RadixTree {
        
        private final Node root = new Node();
        
        void add(String path, Handler handler, String method) {
            if (path == null || path.isEmpty()) path = "/";
            root.insert(path, handler, method, new ArrayList<>(), null);
        }
        
        MatchResult find(String path, String method) {
            if (path == null || path.isEmpty()) path = "/";
            Map<String, String> params = new HashMap<>(4);
            String[] wildcardHolder = new String[2];
            Handler handler = root.search(path, 0, method, params, wildcardHolder);
            
            if (handler == null) return null;
            
            MatchResult result = new MatchResult();
            result.handler = handler;
            result.params = params.isEmpty() ? null : params;
            if (wildcardHolder[0] != null) {
                result.wildcardName = wildcardHolder[0];
                result.wildcardValue = wildcardHolder[1];
            }
            return result;
        }
        
        boolean hasPath(String path) {
            if (path == null || path.isEmpty()) path = "/";
            return root.hasPath(path, 0);
        }
        
        Set<String> getAllowedMethods(String path) {
            if (path == null || path.isEmpty()) path = "/";
            Set<String> methods = new LinkedHashSet<>();
            root.collectMethods(path, 0, methods);
            return methods;
        }
        
        static class MatchResult {
            Handler handler;
            Map<String, String> params;
            String wildcardName;
            String wildcardValue;
        }
        
        private static class Node {
            String path = "";
            Map<String, Handler> handlers;
            List<Node> children;
            String paramName;
            Node paramChild;
            String wildcardName;
            Map<String, Handler> wildcardHandlers;
            
            void insert(String fullPath, Handler handler, String method, 
                       List<String> paramNames, String wildcard) {
                insert(fullPath, 0, handler, method, paramNames, wildcard);
            }
            
            private void insert(String fullPath, int start, Handler handler, String method,
                               List<String> paramNames, String wildcard) {
                if (start >= fullPath.length()) {
                    if (wildcard != null) {
                        if (wildcardHandlers == null) wildcardHandlers = new HashMap<>();
                        wildcardHandlers.put(method, handler);
                        wildcardName = wildcard;
                    } else {
                        if (handlers == null) handlers = new HashMap<>();
                        handlers.put(method, handler);
                    }
                    return;
                }
                
                char c = fullPath.charAt(start);
                
                if (c == '/') {
                    insert(fullPath, start + 1, handler, method, paramNames, wildcard);
                    return;
                }
                
                if (c == ':') {
                    int end = findSegmentEnd(fullPath, start + 1);
                    String pName = fullPath.substring(start + 1, end);
                    paramNames.add(pName);
                    
                    if (paramChild == null) {
                        paramChild = new Node();
                        paramChild.paramName = pName;
                    }
                    paramChild.insert(fullPath, end, handler, method, paramNames, wildcard);
                    return;
                }
                
                if (c == '*') {
                    String wName = fullPath.substring(start + 1);
                    if (wName.isEmpty()) wName = "wildcard";
                    if (wildcardHandlers == null) wildcardHandlers = new HashMap<>();
                    wildcardHandlers.put(method, handler);
                    wildcardName = wName;
                    return;
                }
                
                int end = findSegmentEnd(fullPath, start);
                String segment = fullPath.substring(start, end);
                
                if (segment.isEmpty()) {
                    insert(fullPath, end, handler, method, paramNames, wildcard);
                    return;
                }
                
                Node child = findChild(segment.charAt(0));
                if (child == null) {
                    child = new Node();
                    child.path = segment;
                    addChild(child);
                    child.insert(fullPath, end, handler, method, paramNames, wildcard);
                } else {
                    int common = commonPrefix(child.path, segment);
                    if (common < child.path.length()) {
                        Node split = new Node();
                        split.path = child.path.substring(common);
                        split.handlers = child.handlers;
                        split.children = child.children;
                        split.paramChild = child.paramChild;
                        split.wildcardName = child.wildcardName;
                        split.wildcardHandlers = child.wildcardHandlers;
                        
                        child.path = child.path.substring(0, common);
                        child.handlers = null;
                        child.children = null;
                        child.paramChild = null;
                        child.wildcardName = null;
                        child.wildcardHandlers = null;
                        child.addChild(split);
                    }
                    
                    if (common < segment.length()) {
                        String remaining = segment.substring(common);
                        Node next = child.findChild(remaining.charAt(0));
                        if (next == null) {
                            next = new Node();
                            next.path = remaining;
                            child.addChild(next);
                            next.insert(fullPath, end, handler, method, paramNames, wildcard);
                        } else {
                            // 递归处理：remaining 和 next.path 可能有共同前缀
                            int nextCommon = commonPrefix(next.path, remaining);
                            if (nextCommon < next.path.length()) {
                                // 需要分裂 next 节点
                                Node split = new Node();
                                split.path = next.path.substring(nextCommon);
                                split.handlers = next.handlers;
                                split.children = next.children;
                                split.paramChild = next.paramChild;
                                split.wildcardName = next.wildcardName;
                                split.wildcardHandlers = next.wildcardHandlers;
                                
                                next.path = next.path.substring(0, nextCommon);
                                next.handlers = null;
                                next.children = null;
                                next.paramChild = null;
                                next.wildcardName = null;
                                next.wildcardHandlers = null;
                                next.addChild(split);
                            }
                            
                            if (nextCommon < remaining.length()) {
                                String nextRemaining = remaining.substring(nextCommon);
                                Node newNode = next.findChild(nextRemaining.charAt(0));
                                if (newNode == null) {
                                    newNode = new Node();
                                    newNode.path = nextRemaining;
                                    next.addChild(newNode);
                                }
                                newNode.insert(fullPath, start + common + nextCommon + nextRemaining.length(), 
                                              handler, method, paramNames, wildcard);
                            } else {
                                next.insert(fullPath, start + common + remaining.length(), 
                                           handler, method, paramNames, wildcard);
                            }
                        }
                    } else {
                        child.insert(fullPath, end, handler, method, paramNames, wildcard);
                    }
                }
            }
            
            Handler search(String path, int start, String method, 
                          Map<String, String> params, String[] wildcardHolder) {
                if (start >= path.length()) return getHandler(method);
                
                char c = path.charAt(start);
                if (c == '/') return search(path, start + 1, method, params, wildcardHolder);
                
                if (children != null) {
                    for (Node child : children) {
                        if (child.path.isEmpty()) continue;
                        int len = child.path.length();
                        if (child.path.charAt(0) == c && path.regionMatches(start, child.path, 0, len)) {
                            Handler h = child.search(path, start + len, method, params, wildcardHolder);
                            if (h != null) return h;
                        }
                    }
                }
                
                if (paramChild != null) {
                    int end = path.indexOf('/', start);
                    if (end == -1) end = path.length();
                    String value = path.substring(start, end);
                    if (!value.isEmpty()) {
                        params.put(paramChild.paramName, value);
                        Handler h = paramChild.search(path, end, method, params, wildcardHolder);
                        if (h != null) return h;
                        params.remove(paramChild.paramName);
                    }
                }
                
                if (wildcardHandlers != null) {
                    Handler h = wildcardHandlers.get(method);
                    if (h == null) h = wildcardHandlers.get("ANY");
                    if (h != null) {
                        wildcardHolder[0] = wildcardName;
                        wildcardHolder[1] = path.substring(start);
                        return h;
                    }
                }
                
                return null;
            }
            
            boolean hasPath(String path, int start) {
                if (start >= path.length()) return handlers != null || wildcardHandlers != null;
                char c = path.charAt(start);
                
                if (children != null) {
                    for (Node child : children) {
                        if (child.path.isEmpty()) continue;
                        int len = child.path.length();
                        if (child.path.charAt(0) == c && path.regionMatches(start, child.path, 0, len)) {
                            if (child.hasPath(path, start + len)) return true;
                        }
                    }
                }
                
                if (paramChild != null) {
                    int end = path.indexOf('/', start);
                    if (end == -1) end = path.length();
                    if (end > start && paramChild.hasPath(path, end)) return true;
                }
                
                return wildcardHandlers != null;
            }
            
            void collectMethods(String path, int start, Set<String> methods) {
                if (start >= path.length()) {
                    collectHandlerMethods(handlers, methods);
                    collectHandlerMethods(wildcardHandlers, methods);
                    return;
                }
                
                char c = path.charAt(start);
                
                if (children != null) {
                    for (Node child : children) {
                        if (child.path.isEmpty()) continue;
                        int len = child.path.length();
                        if (child.path.charAt(0) == c && path.regionMatches(start, child.path, 0, len)) {
                            child.collectMethods(path, start + len, methods);
                        }
                    }
                }
                
                if (paramChild != null) {
                    int end = path.indexOf('/', start);
                    if (end == -1) end = path.length();
                    if (end > start) paramChild.collectMethods(path, end, methods);
                }
                
                collectHandlerMethods(wildcardHandlers, methods);
            }
            
            private void collectHandlerMethods(Map<String, Handler> h, Set<String> methods) {
                if (h == null) return;
                for (String m : h.keySet()) {
                    if ("ANY".equals(m)) {
                        methods.addAll(Arrays.asList("GET","POST","PUT","DELETE","PATCH","HEAD","OPTIONS"));
                    } else {
                        methods.add(m);
                    }
                }
            }
            
            private Handler getHandler(String method) {
                if (handlers == null) return null;
                Handler h = handlers.get(method);
                if (h == null) h = handlers.get("ANY");
                return h;
            }
            
            private Node findChild(char c) {
                if (children == null) return null;
                for (Node child : children) {
                    if (child.path.length() > 0 && child.path.charAt(0) == c) return child;
                }
                return null;
            }
            
            private void addChild(Node child) {
                if (children == null) children = new ArrayList<>(4);
                children.add(child);
            }
            
            private int findSegmentEnd(String path, int start) {
                for (int i = start; i < path.length(); i++) {
                    char c = path.charAt(i);
                    if (c == '/' || c == ':' || c == '*') return i;
                }
                return path.length();
            }
            
            private int commonPrefix(String a, String b) {
                int max = Math.min(a.length(), b.length());
                int i = 0;
                while (i < max && a.charAt(i) == b.charAt(i)) i++;
                return i;
            }
        }
    }
}
