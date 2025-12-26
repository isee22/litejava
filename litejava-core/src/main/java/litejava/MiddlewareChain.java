package litejava;

import java.util.*;

/**
 * Executes middleware chain in onion model (Koa-style).
 * Each middleware can execute code before and after calling next().
 * Supports abort() to stop the chain.
 */
class MiddlewareChain {
    
    private final List<MiddlewarePlugin> middlewares;
    private final Handler finalHandler;
    private final int size;
    
    MiddlewareChain(List<MiddlewarePlugin> middlewares, Handler finalHandler) {
        // 直接引用，不复制
        this.middlewares = middlewares;
        this.finalHandler = finalHandler;
        this.size = middlewares.size();
    }
    
    /**
     * Execute the middleware chain with the given context.
     */
    void execute(Context ctx) throws Exception {
        executeAt(ctx, 0);
    }
    
    private void executeAt(Context ctx, int index) throws Exception {
        // 检查是否已中断
        if (ctx.isAborted()) {
            return;
        }
        
        if (index < size) {
            middlewares.get(index).handle(ctx, () -> executeAt(ctx, index + 1));
        } else if (finalHandler != null) {
            finalHandler.handle(ctx);
        }
    }
    
    /**
     * Create a chain with additional middlewares prepended.
     */
    MiddlewareChain prepend(List<MiddlewarePlugin> additional) {
        List<MiddlewarePlugin> combined = new ArrayList<>(additional.size() + size);
        combined.addAll(additional);
        combined.addAll(middlewares);
        return new MiddlewareChain(combined, finalHandler);
    }
}
