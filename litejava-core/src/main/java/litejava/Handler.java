package litejava;

/**
 * 请求处理器函数式接口
 * 
 * <p>Handler 是 LiteJava 处理 HTTP 请求的核心接口，
 * 采用函数式设计，可以使用 Lambda 表达式简洁地定义处理逻辑。
 * 
 * <h2>基本用法</h2>
 * <pre>{@code
 * // Lambda 表达式
 * app.get("/hello", ctx -> ctx.text("Hello World"));
 * 
 * // 方法引用
 * app.get("/users", userController::list);
 * app.post("/users", userController::create);
 * 
 * // 匿名类（不推荐）
 * app.get("/old", new Handler() {
 *     public void handle(Context ctx) {
 *         ctx.text("Old style");
 *     }
 * });
 * }</pre>
 * 
 * <h2>异常处理</h2>
 * <pre>{@code
 * app.get("/users/:id", ctx -> {
 *     long id = ctx.paramLong("id");
 *     User user = userService.get(id);
 *     if (user == null) {
 *         throw new NotFoundException("User not found");
 *     }
 *     ctx.json(user);
 * });
 * }</pre>
 * 
 * <h2>Controller 风格</h2>
 * <pre>{@code
 * public class UserController {
 *     public void list(Context ctx) {
 *         ctx.json(userService.list());
 *     }
 *     
 *     public void get(Context ctx) {
 *         long id = ctx.paramLong("id");
 *         ctx.json(userService.get(id));
 *     }
 *     
 *     public void create(Context ctx) {
 *         Map<String, Object> data = ctx.bindJSON();
 *         ctx.ok(userService.create(data));
 *     }
 * }
 * 
 * // 注册路由
 * UserController uc = new UserController();
 * app.get("/users", uc::list);
 * app.get("/users/:id", uc::get);
 * app.post("/users", uc::create);
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see Context 请求上下文
 * @see App 应用容器
 */
@FunctionalInterface
public interface Handler {
    
    /**
     * 处理 HTTP 请求
     * 
     * @param ctx 请求上下文，包含请求信息和响应方法
     * @throws Exception 处理过程中的异常，会被全局错误处理器捕获
     */
    void handle(Context ctx) throws Exception;
}
