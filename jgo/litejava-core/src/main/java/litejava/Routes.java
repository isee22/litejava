package litejava;

import java.util.*;

/**
 * 路由收集器 - 批量定义路由的 fluent API
 * 
 * <pre>{@code
 * public class AuthRoutes {
 *     public Routes routes() {
 *         return new Routes()
 *             .post("/api/auth/login", this::login)
 *                 .summary("用户登录").tags("认证")
 *             .post("/api/auth/logout", this::logout)
 *                 .summary("用户登出").tags("认证")
 *             .get("/api/auth/me", this::me)
 *                 .summary("获取当前用户").tags("认证");
 *     }
 * }
 * 
 * // 注册到 App:
 * app.register(new AuthRoutes().routes());
 * }</pre>
 */
public class Routes {
    
    public final List<Route> routes = new ArrayList<>();
    
    /** @deprecated Use routes instead */
    @Deprecated
    public final List<Entry> entries = new ArrayList<>();
    
    /** @deprecated Use Route instead */
    @Deprecated
    public static class Entry {
        public String method;
        public String path;
        public Handler handler;
        
        public Entry(String method, String path, Handler handler) {
            this.method = method;
            this.path = path;
            this.handler = handler;
        }
    }
    
    public RouteBuilder get(String path, Handler handler) {
        return add("GET", path, handler);
    }
    
    public RouteBuilder post(String path, Handler handler) {
        return add("POST", path, handler);
    }
    
    public RouteBuilder put(String path, Handler handler) {
        return add("PUT", path, handler);
    }
    
    public RouteBuilder delete(String path, Handler handler) {
        return add("DELETE", path, handler);
    }
    
    public RouteBuilder patch(String path, Handler handler) {
        return add("PATCH", path, handler);
    }
    
    public RouteBuilder route(String method, String path, Handler handler) {
        return add(method.toUpperCase(), path, handler);
    }
    
    private RouteBuilder add(String method, String path, Handler handler) {
        Route route = new Route(method, path, handler);
        routes.add(route);
        entries.add(new Entry(method, path, handler)); // 兼容旧代码
        return new RouteBuilder(this, route);
    }
    
    /**
     * 路由构建器 - 支持链式添加元数据后继续添加路由
     */
    public static class RouteBuilder {
        private final Routes parent;
        private final Route route;
        
        RouteBuilder(Routes parent, Route route) {
            this.parent = parent;
            this.route = route;
        }
        
        public RouteBuilder summary(String summary) {
            route.summary(summary);
            return this;
        }
        
        public RouteBuilder desc(String description) {
            route.desc(description);
            return this;
        }
        
        public RouteBuilder tags(String... tags) {
            route.tags(tags);
            return this;
        }
        
        public RouteBuilder param(String name, Class<?> type, boolean required, String desc) {
            route.param(name, type, required, desc);
            return this;
        }
        
        public RouteBuilder param(String name, Class<?> type, String desc) {
            route.param(name, type, desc);
            return this;
        }
        
        public RouteBuilder body(Class<?> type, String desc) {
            route.body(type, desc);
            return this;
        }
        
        public RouteBuilder body(Class<?> type) {
            route.body(type);
            return this;
        }
        
        public RouteBuilder response(int code, Class<?> type, String desc) {
            route.response(code, type, desc);
            return this;
        }
        
        public RouteBuilder response(int code, Class<?> type) {
            route.response(code, type);
            return this;
        }
        
        // 继续添加路由的方法
        public RouteBuilder get(String path, Handler handler) {
            return parent.get(path, handler);
        }
        
        public RouteBuilder post(String path, Handler handler) {
            return parent.post(path, handler);
        }
        
        public RouteBuilder put(String path, Handler handler) {
            return parent.put(path, handler);
        }
        
        public RouteBuilder delete(String path, Handler handler) {
            return parent.delete(path, handler);
        }
        
        public RouteBuilder patch(String path, Handler handler) {
            return parent.patch(path, handler);
        }
        
        public RouteBuilder route(String method, String path, Handler handler) {
            return parent.route(method, path, handler);
        }
        
        /** 获取父 Routes 对象 */
        public Routes end() {
            return parent;
        }
    }
}
