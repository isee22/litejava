package litejava;

import java.util.*;

/**
 * 路由收集器 - 批量定义路由的 fluent API
 * 
 * <pre>{@code
 * public class AuthRoutes {
 *     public Routes routes() {
 *         return new Routes(this)  // 传入 controller 实例用于注解扫描
 *             .post("/api/auth/login", this::login)
 *             .post("/api/auth/logout", this::logout)
 *             .get("/api/auth/me", this::me)
 *             .end();
 *     }
 * }
 * 
 * // 注册到 App:
 * app.register(new AuthRoutes().routes());
 * }</pre>
 */
public class Routes {
    
    public final List<Route> routes = new ArrayList<>();
    
    /** Controller 实例（用于注解扫描） */
    public Object controller;
    
    public Routes() {}
    
    /** 创建 Routes 并关联 Controller 实例（推荐，支持注解扫描） */
    public Routes(Object controller) {
        this.controller = controller;
    }
    
    public RouteBuilder get(String path, Handler handler) {
        return add("GET", path, handler, null);
    }
    
    /** GET 路由，指定方法名用于注解扫描 */
    public RouteBuilder get(String path, Handler handler, String methodName) {
        return add("GET", path, handler, methodName);
    }
    
    public RouteBuilder post(String path, Handler handler) {
        return add("POST", path, handler, null);
    }
    
    /** POST 路由，指定方法名用于注解扫描 */
    public RouteBuilder post(String path, Handler handler, String methodName) {
        return add("POST", path, handler, methodName);
    }
    
    public RouteBuilder put(String path, Handler handler) {
        return add("PUT", path, handler, null);
    }
    
    /** PUT 路由，指定方法名用于注解扫描 */
    public RouteBuilder put(String path, Handler handler, String methodName) {
        return add("PUT", path, handler, methodName);
    }
    
    public RouteBuilder delete(String path, Handler handler) {
        return add("DELETE", path, handler, null);
    }
    
    /** DELETE 路由，指定方法名用于注解扫描 */
    public RouteBuilder delete(String path, Handler handler, String methodName) {
        return add("DELETE", path, handler, methodName);
    }
    
    public RouteBuilder patch(String path, Handler handler) {
        return add("PATCH", path, handler, null);
    }
    
    /** PATCH 路由，指定方法名用于注解扫描 */
    public RouteBuilder patch(String path, Handler handler, String methodName) {
        return add("PATCH", path, handler, methodName);
    }
    
    public RouteBuilder route(String method, String path, Handler handler) {
        return add(method.toUpperCase(), path, handler, null);
    }
    
    RouteBuilder add(String method, String path, Handler handler, String methodName) {
        Route route = new Route(method, path, handler);
        
        // 保存 controller 信息用于注解扫描
        if (controller != null) {
            route.controllerClass = controller.getClass();
            route.methodName = methodName != null ? methodName : extractMethodName(handler);
        }
        
        routes.add(route);
        return new RouteBuilder(this, route);
    }
    
    /** 从 Handler lambda 提取方法名（通过 SerializedLambda） */
    String extractMethodName(Handler handler) {
        try {
            java.lang.reflect.Method writeReplace = handler.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            java.lang.invoke.SerializedLambda lambda = (java.lang.invoke.SerializedLambda) writeReplace.invoke(handler);
            return lambda.getImplMethodName();
        } catch (Exception e) {
            // 无法提取方法名，返回 null
            return null;
        }
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
        
        public RouteBuilder get(String path, Handler handler, String methodName) {
            return parent.get(path, handler, methodName);
        }
        
        public RouteBuilder post(String path, Handler handler) {
            return parent.post(path, handler);
        }
        
        public RouteBuilder post(String path, Handler handler, String methodName) {
            return parent.post(path, handler, methodName);
        }
        
        public RouteBuilder put(String path, Handler handler) {
            return parent.put(path, handler);
        }
        
        public RouteBuilder put(String path, Handler handler, String methodName) {
            return parent.put(path, handler, methodName);
        }
        
        public RouteBuilder delete(String path, Handler handler) {
            return parent.delete(path, handler);
        }
        
        public RouteBuilder delete(String path, Handler handler, String methodName) {
            return parent.delete(path, handler, methodName);
        }
        
        public RouteBuilder patch(String path, Handler handler) {
            return parent.patch(path, handler);
        }
        
        public RouteBuilder patch(String path, Handler handler, String methodName) {
            return parent.patch(path, handler, methodName);
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
