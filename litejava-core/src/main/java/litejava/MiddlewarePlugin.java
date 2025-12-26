package litejava;

/**
 * 中间件插件 - Koa-style 洋葱模型
 * 
 * <p>中间件是 LiteJava 处理横切关注点的核心机制，借鉴 Koa.js 的洋葱模型：
 * <ul>
 *   <li>请求从外到内依次经过中间件</li>
 *   <li>响应从内到外依次返回</li>
 *   <li>每个中间件可以在 next.run() 前后执行逻辑</li>
 * </ul>
 * 
 * <h2>洋葱模型示意</h2>
 * <pre>
 *     ┌─────────────────────────────────────┐
 *     │           Middleware 1              │
 *     │   ┌─────────────────────────────┐   │
 *     │   │       Middleware 2          │   │
 *     │   │   ┌─────────────────────┐   │   │
 *     │   │   │    Middleware 3     │   │   │
 *     │   │   │   ┌─────────────┐   │   │   │
 * ──▶ │ ──▶ │ ──▶ │   Handler   │ ──▶ │ ──▶ │ ──▶
 *     │   │   │   └─────────────┘   │   │   │
 *     │   │   └─────────────────────┘   │   │
 *     │   └─────────────────────────────┘   │
 *     └─────────────────────────────────────┘
 * </pre>
 * 
 * <h2>基本用法</h2>
 * <pre>{@code
 * public class LogMiddleware extends MiddlewarePlugin {
 *     @Override
 *     public void handle(Context ctx, Next next) throws Exception {
 *         long start = System.currentTimeMillis();
 *         
 *         next.run();  // 执行后续中间件和 handler
 *         
 *         long cost = System.currentTimeMillis() - start;
 *         System.out.println(ctx.method + " " + ctx.path + " " + cost + "ms");
 *     }
 * }
 * }</pre>
 * 
 * <h2>认证中间件示例</h2>
 * <pre>{@code
 * public class AuthMiddleware extends MiddlewarePlugin {
 *     @Override
 *     public void handle(Context ctx, Next next) throws Exception {
 *         String token = ctx.header("Authorization");
 *         
 *         if (token == null || !validateToken(token)) {
 *             ctx.abortWithJson(401, Map.of("error", "Unauthorized"));
 *             return;  // 不调用 next.run()，中断请求
 *         }
 *         
 *         // 将用户信息存入 state，供后续 handler 使用
 *         ctx.state.put("user", getUserFromToken(token));
 *         
 *         next.run();  // 继续执行
 *     }
 * }
 * }</pre>
 * 
 * <h2>异常恢复中间件</h2>
 * <pre>{@code
 * public class RecoveryMiddleware extends MiddlewarePlugin {
 *     @Override
 *     public void handle(Context ctx, Next next) throws Exception {
 *         try {
 *             next.run();
 *         } catch (Exception e) {
 *             log.error("Request failed", e);
 *             ctx.status(500).json(Map.of("error", "Internal Server Error"));
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h2>注册中间件（按注册顺序执行，与 Gin 一致）</h2>
 * <pre>{@code
 * // 推荐顺序：
 * app.use(new RecoveryPlugin());     // 1. 异常恢复（最外层）
 * app.use(new RequestLogPlugin());   // 2. 请求日志
 * app.use(new CorsPlugin());         // 3. 跨域处理
 * app.use(new AuthMiddleware());     // 4. 认证
 * // ... 其他中间件
 * 
 * // 静态文件建议用路由方式，而非中间件
 * app.get("/static/*filepath", staticHandler);
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see Next 中间件链继续执行接口
 * @see Context#abort() 中断请求
 * @see Context#isAborted() 检查是否已中断
 */
public abstract class MiddlewarePlugin extends Plugin {
    
    /**
     * 处理请求（Koa-style 洋葱模型）
     * 
     * <p>实现要点：
     * <ul>
     *   <li>调用 next.run() 执行后续中间件和 handler</li>
     *   <li>不调用 next.run() 则中断请求链</li>
     *   <li>可以在 next.run() 前后分别执行逻辑</li>
     *   <li>使用 ctx.abort() 标记请求已中断</li>
     * </ul>
     * 
     * @param ctx 请求上下文
     * @param next 中间件链继续执行接口
     * @throws Exception 处理异常
     */
    public abstract void handle(Context ctx, Next next) throws Exception;
}
