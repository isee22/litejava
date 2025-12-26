package litejava.plugins.vt;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 通用对象池 (类似 Go sync.Pool)
 * 
 * <pre>{@code
 * // 创建 Context 池
 * ObjectPool<Context> pool = new ObjectPool<>(Context::new, ctx -> {
 *     ctx.app = null;
 *     ctx.method = null;
 *     ctx.headers.clear();
 * });
 * 
 * // 使用
 * Context ctx = pool.acquire();
 * try {
 *     // ... 处理请求
 * } finally {
 *     pool.release(ctx);
 * }
 * }</pre>
 */
public class ObjectPool<T> {
    
    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final Consumer<T> resetter;
    private final int maxSize;
    
    /**
     * @param factory  创建新对象的工厂
     * @param resetter 重置对象状态的函数
     */
    public ObjectPool(Supplier<T> factory, Consumer<T> resetter) {
        this(factory, resetter, 1024);
    }
    
    public ObjectPool(Supplier<T> factory, Consumer<T> resetter, int maxSize) {
        this.factory = factory;
        this.resetter = resetter;
        this.maxSize = maxSize;
    }
    
    /**
     * 从池中获取对象，池空则创建新的
     */
    public T acquire() {
        T obj = pool.poll();
        return obj != null ? obj : factory.get();
    }
    
    /**
     * 归还对象到池中
     */
    public void release(T obj) {
        if (obj == null) return;
        if (pool.size() < maxSize) {
            resetter.accept(obj);
            pool.offer(obj);
        }
        // 超过 maxSize 则丢弃，让 GC 回收
    }
    
    /**
     * 当前池大小
     */
    public int size() {
        return pool.size();
    }
    
    /**
     * 清空池
     */
    public void clear() {
        pool.clear();
    }
}
