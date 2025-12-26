package litejava.plugins.router;

import litejava.Handler;
import litejava.Route;
import litejava.plugin.RouterPlugin;

import java.util.*;
import java.util.function.Consumer;

/**
 * 简单 HashMap 路由插件
 * 
 * <p>适用场景：
 * <ul>
 *   <li>路由数量少（< 50）</li>
 *   <li>全部是静态路由，无参数</li>
 *   <li>追求最简单的实现</li>
 * </ul>
 * 
 * <p>特点：
 * <ul>
 *   <li>O(1) 精确匹配</li>
 *   <li>不支持路径参数 :id</li>
 *   <li>不支持通配符 *filepath</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new SimpleRouterPlugin());
 * app.get("/users", ctx -> ctx.json(users));
 * app.get("/about", ctx -> ctx.text("About"));
 * }</pre>
 */
public class SimpleRouterPlugin extends RouterPlugin {
    
    // method:path -> handler
    private final Map<String, Handler> routes = new HashMap<>();
    private final List<Route> routeList = new ArrayList<>();
    
    public SimpleRouterPlugin() {}
    
    protected SimpleRouterPlugin(String prefix, SimpleRouterPlugin parent) {
        this.prefix = prefix;
        this.parent = parent;
    }
    
    private SimpleRouterPlugin parent;
    
    @Override
    public Route route(String method, String path, Handler handler) {
        String fullPath = normalizePath(prefix + path);
        String key = method + ":" + fullPath;
        
        SimpleRouterPlugin root = getSimpleRoot();
        root.routes.put(key, handler);
        
        Route route = new Route(method, fullPath, handler);
        root.routeList.add(route);
        
        return route;
    }
    
    @Override
    public RouteMatch match(String method, String path) {
        // 精确匹配
        Handler handler = routes.get(method + ":" + path);
        if (handler == null) {
            handler = routes.get("ANY:" + path);
        }
        
        if (handler == null) return null;
        
        RouteMatch match = new RouteMatch();
        match.handler = handler;
        return match;
    }
    
    @Override
    public boolean hasPath(String path) {
        for (String key : routes.keySet()) {
            if (key.endsWith(":" + path)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Set<String> getAllowedMethods(String path) {
        Set<String> methods = new LinkedHashSet<>();
        for (String key : routes.keySet()) {
            if (key.endsWith(":" + path)) {
                String method = key.substring(0, key.indexOf(':'));
                if ("ANY".equals(method)) {
                    methods.addAll(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"));
                } else {
                    methods.add(method);
                }
            }
        }
        return methods;
    }
    
    @Override
    public List<Route> getAllRoutes() {
        return new ArrayList<>(routeList);
    }
    
    @Override
    public RouterPlugin group(String groupPrefix) {
        return new SimpleRouterPlugin(normalizePath(prefix + groupPrefix), getSimpleRoot());
    }
    
    @Override
    public RouterPlugin group(String groupPrefix, Consumer<RouterPlugin> configure) {
        SimpleRouterPlugin subRouter = new SimpleRouterPlugin(normalizePath(prefix + groupPrefix), getSimpleRoot());
        configure.accept(subRouter);
        return this;
    }
    
    private SimpleRouterPlugin getSimpleRoot() {
        return parent != null ? parent.getSimpleRoot() : this;
    }
}
