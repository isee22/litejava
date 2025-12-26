package litejava;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for MiddlewarePlugin (中间件插件).
 */
class MiddlewarePropertyTest {
    
    /**
     * 创建中间件的辅助方法
     */
    private MiddlewarePlugin middleware(MiddlewareHandler handler) {
        return new MiddlewarePlugin() {
            @Override
            public void handle(Context ctx, Next next) throws Exception {
                handler.handle(ctx, next);
            }
        };
    }
    
    @FunctionalInterface
    interface MiddlewareHandler {
        void handle(Context ctx, Next next) throws Exception;
    }
    
    /**
     * **Feature: lite-java-framework, Property 2: Middleware Execution Order (Onion Model)**
     * **Validates: Requirements 2.1, 2.2, 2.3**
     * 
     * For any sequence of N middlewares registered in order [M1, M2, ..., Mn]:
     * - The "before next" code executes in order: M1 → M2 → ... → Mn
     * - The "after next" code executes in reverse order: Mn → ... → M2 → M1
     */
    @Property(tries = 100)
    void middlewareExecutionOrder(@ForAll @IntRange(min = 1, max = 10) int count) throws Exception {
        List<Integer> executionOrder = new ArrayList<>();
        List<MiddlewarePlugin> middlewares = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            final int index = i;
            middlewares.add(middleware((ctx, next) -> {
                executionOrder.add(index);  // before next
                next.run();
                executionOrder.add(count + index);  // after next
            }));
        }
        
        MiddlewareChain chain = new MiddlewareChain(middlewares, ctx -> {});
        Context ctx = new Context();
        chain.execute(ctx);
        
        // Verify onion model:
        // Before: 0, 1, 2, ..., count-1
        // After: count+(count-1), count+(count-2), ..., count+0
        assertEquals(count * 2, executionOrder.size(), "Should have 2*count entries");
        
        // Check "before" order: 0, 1, 2, ..., count-1
        for (int i = 0; i < count; i++) {
            assertEquals(i, executionOrder.get(i), "Before order should be sequential");
        }
        
        // Check "after" order: count+(count-1), count+(count-2), ..., count+0
        for (int i = 0; i < count; i++) {
            int expectedAfterValue = count + (count - 1 - i);
            assertEquals(expectedAfterValue, executionOrder.get(count + i), 
                "After order should be reverse");
        }
    }
    
    /**
     * **Feature: lite-java-framework, Property 3: Middleware Chain Termination**
     * **Validates: Requirements 2.4**
     * 
     * For any middleware chain where middleware Mi does not call next(),
     * middlewares Mi+1 through Mn should not execute.
     */
    @Property(tries = 100)
    void middlewareChainTermination(
            @ForAll @IntRange(min = 2, max = 10) int totalCount,
            @ForAll @IntRange(min = 0, max = 9) int stopAt) {
        
        Assume.that(stopAt < totalCount);
        
        List<Integer> executionOrder = new ArrayList<>();
        List<MiddlewarePlugin> middlewares = new ArrayList<>();
        
        for (int i = 0; i < totalCount; i++) {
            final int index = i;
            if (i == stopAt) {
                // This middleware does NOT call next()
                middlewares.add(middleware((ctx, next) -> {
                    executionOrder.add(index);
                    // Intentionally not calling next.run()
                }));
            } else {
                middlewares.add(middleware((ctx, next) -> {
                    executionOrder.add(index);
                    next.run();
                    executionOrder.add(1000 + index);  // after marker
                }));
            }
        }
        
        MiddlewareChain chain = new MiddlewareChain(middlewares, ctx -> {
            executionOrder.add(999);  // handler marker
        });
        
        try {
            Context ctx = new Context();
            chain.execute(ctx);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        
        // Middlewares after stopAt should not execute
        for (int i = stopAt + 1; i < totalCount; i++) {
            assertFalse(executionOrder.contains(i), 
                "Middleware " + i + " should not execute after chain termination at " + stopAt);
        }
        
        // Handler should not execute
        assertFalse(executionOrder.contains(999), 
            "Handler should not execute when chain is terminated");
        
        // Middlewares before stopAt should have executed
        for (int i = 0; i <= stopAt; i++) {
            assertTrue(executionOrder.contains(i), 
                "Middleware " + i + " should have executed");
        }
    }

    /**
     * **Feature: lite-java-framework, Property 25: Middleware Exception Propagation**
     * **Validates: Requirements 2.5**
     * 
     * For any middleware that throws an exception E, the exception should propagate
     * to the error handling middleware with the original exception information preserved.
     */
    @Property(tries = 100)
    void middlewareExceptionPropagation(
            @ForAll @IntRange(min = 1, max = 5) int throwAt,
            @ForAll @StringLength(min = 1, max = 50) String errorMessage) {
        
        List<MiddlewarePlugin> middlewares = new ArrayList<>();
        
        for (int i = 0; i < throwAt + 2; i++) {
            final int index = i;
            if (i == throwAt) {
                middlewares.add(middleware((ctx, next) -> {
                    throw new RuntimeException(errorMessage);
                }));
            } else {
                middlewares.add(middleware((ctx, next) -> {
                    next.run();
                }));
            }
        }
        
        MiddlewareChain chain = new MiddlewareChain(middlewares, ctx -> {});
        Context ctx = new Context();
        
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            chain.execute(ctx);
        });
        
        assertEquals(errorMessage, thrown.getMessage(), 
            "Exception message should be preserved");
    }
    
    /**
     * Test that context modifications are visible across middlewares.
     */
    @Property(tries = 100)
    void contextModificationsAreVisible(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String key,
            @ForAll @StringLength(max = 50) String value) {
        
        List<MiddlewarePlugin> middlewares = new ArrayList<>();
        final boolean[] valueFound = {false};
        
        // First middleware sets a value
        middlewares.add(middleware((ctx, next) -> {
            ctx.state.put(key, value);
            next.run();
        }));
        
        // Second middleware reads the value
        middlewares.add(middleware((ctx, next) -> {
            valueFound[0] = value.equals(ctx.state.get(key));
            next.run();
        }));
        
        MiddlewareChain chain = new MiddlewareChain(middlewares, ctx -> {});
        
        try {
            Context ctx = new Context();
            chain.execute(ctx);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        
        assertTrue(valueFound[0], "Value set by first middleware should be visible to second");
    }
}
