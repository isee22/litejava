package litejava;

import java.util.concurrent.CompletableFuture;

/**
 * 异步处理器 - 支持 CompletableFuture
 */
@FunctionalInterface
public interface AsyncHandler {
    CompletableFuture<Void> handle(Context ctx);
}
