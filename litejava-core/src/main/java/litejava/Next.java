package litejava;

/**
 * Functional interface for the next() function in middleware chain.
 */
@FunctionalInterface
public interface Next {
    void run() throws Exception;
}
