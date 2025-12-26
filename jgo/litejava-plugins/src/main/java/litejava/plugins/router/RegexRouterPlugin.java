package litejava.plugins.router;

import litejava.Handler;
import litejava.Route;
import litejava.plugin.RouterPlugin;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式路由插件
 * 
 * <p>适用场景：
 * <ul>
 *   <li>需要复杂的路径匹配规则</li>
 *   <li>需要自定义参数格式验证</li>
 *   <li>路由数量较少，性能要求不高</li>
 * </ul>
 * 
 * <p>特点：
 * <ul>
 *   <li>支持完整正则表达式</li>
 *   <li>支持参数格式约束：{@code /users/:id<\\d+>}</li>
 *   <li>O(n) 线性匹配，n 为路由数量</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new RegexRouterPlugin());
 * 
 * // 标准参数
 * app.get("/users/:id", ctx -> ctx.json(user));
 * 
 * // 带格式约束的参数（只匹配数字）
 * app.get("/posts/:id<\\d+>", ctx -> ctx.json(post));
 * 
 * // 通配符
 * app.get("/files/*filepath", ctx -> ctx.file(path));
 * }</pre>
 */
public class RegexRouterPlugin extends RouterPlugin {
    
    private final List<RegexRoute> regexRoutes = new ArrayList<>();
    private final List<Route> routeList = new ArrayList<>();
    private RegexRouterPlugin parent;
    
    public RegexRouterPlugin() {}
    
    protected RegexRouterPlugin(String prefix, RegexRouterPlugin parent) {
        this.prefix = prefix;
        this.parent = parent;
    }
    
    @Override
    public Route route(String method, String path, Handler handler) {
        String fullPath = normalizePath(prefix + path);
        
        RegexRouterPlugin root = getRegexRoot();
        root.regexRoutes.add(new RegexRoute(method, fullPath, handler));
        
        Route route = new Route(method, fullPath, handler);
        root.routeList.add(route);
        
        return route;
    }
    
    @Override
    public RouteMatch match(String method, String path) {
        for (RegexRoute route : regexRoutes) {
            if (!route.method.equals(method) && !route.method.equals("ANY")) {
                continue;
            }
            
            Matcher matcher = route.pattern.matcher(path);
            if (matcher.matches()) {
                RouteMatch match = new RouteMatch();
                match.handler = route.handler;
                
                // 提取参数
                if (!route.paramNames.isEmpty()) {
                    match.params = new HashMap<>();
                    for (int i = 0; i < route.paramNames.size(); i++) {
                        String name = route.paramNames.get(i);
                        String value = matcher.group(i + 1);
                        if (name.equals(route.wildcardName)) {
                            match.wildcardName = name;
                            match.wildcardValue = value;
                        }
                        match.params.put(name, value);
                    }
                }
                
                return match;
            }
        }
        return null;
    }
    
    @Override
    public boolean hasPath(String path) {
        for (RegexRoute route : regexRoutes) {
            if (route.pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Set<String> getAllowedMethods(String path) {
        Set<String> methods = new LinkedHashSet<>();
        for (RegexRoute route : regexRoutes) {
            if (route.pattern.matcher(path).matches()) {
                if ("ANY".equals(route.method)) {
                    methods.addAll(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"));
                } else {
                    methods.add(route.method);
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
        return new RegexRouterPlugin(normalizePath(prefix + groupPrefix), getRegexRoot());
    }
    
    @Override
    public RouterPlugin group(String groupPrefix, Consumer<RouterPlugin> configure) {
        RegexRouterPlugin subRouter = new RegexRouterPlugin(normalizePath(prefix + groupPrefix), getRegexRoot());
        configure.accept(subRouter);
        return this;
    }
    
    private RegexRouterPlugin getRegexRoot() {
        return parent != null ? parent.getRegexRoot() : this;
    }
    
    /**
     * 正则路由条目
     */
    private static class RegexRoute {
        final String method;
        final String originalPath;
        final Handler handler;
        final Pattern pattern;
        final List<String> paramNames;
        final String wildcardName;
        
        RegexRoute(String method, String path, Handler handler) {
            this.method = method;
            this.originalPath = path;
            this.handler = handler;
            this.paramNames = new ArrayList<>();
            
            String wildcardTemp = null;
            StringBuilder regex = new StringBuilder("^");
            int i = 0;
            
            while (i < path.length()) {
                char c = path.charAt(i);
                
                if (c == ':') {
                    // 参数 :name 或 :name<pattern>
                    int start = i + 1;
                    int end = start;
                    while (end < path.length() && isParamChar(path.charAt(end))) {
                        end++;
                    }
                    String paramName = path.substring(start, end);
                    paramNames.add(paramName);
                    
                    // 检查是否有自定义正则
                    if (end < path.length() && path.charAt(end) == '<') {
                        int closeIdx = path.indexOf('>', end);
                        if (closeIdx > end) {
                            String customPattern = path.substring(end + 1, closeIdx);
                            regex.append("(").append(customPattern).append(")");
                            i = closeIdx + 1;
                            continue;
                        }
                    }
                    
                    regex.append("([^/]+)");
                    i = end;
                } else if (c == '*') {
                    // 通配符 *name
                    int start = i + 1;
                    int end = start;
                    while (end < path.length() && isParamChar(path.charAt(end))) {
                        end++;
                    }
                    wildcardTemp = end > start ? path.substring(start, end) : "wildcard";
                    paramNames.add(wildcardTemp);
                    regex.append("(.*)");
                    i = end;
                } else {
                    // 转义正则特殊字符
                    if ("[]{}()^$.|*+?\\".indexOf(c) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(c);
                    i++;
                }
            }
            
            regex.append("$");
            this.pattern = Pattern.compile(regex.toString());
            this.wildcardName = wildcardTemp;
        }
        
        private boolean isParamChar(char c) {
            return Character.isLetterOrDigit(c) || c == '_';
        }
    }
}
