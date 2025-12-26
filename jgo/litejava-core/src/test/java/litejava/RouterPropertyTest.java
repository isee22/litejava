package litejava;

import litejava.plugin.RouterPlugin;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for RouterPlugin (Radix Tree implementation).
 */
class RouterPropertyTest {
    
    /**
     * **Feature: lite-java-framework, Property 5: Path Parameter Extraction**
     * **Validates: Requirements 3.2, 4.5**
     * 
     * For any route pattern with parameters and a matching request path,
     * all parameter values should be correctly extracted and accessible by name.
     */
    @Property(tries = 100)
    void pathParameterExtraction(
            @ForAll @Size(min = 1, max = 3) List<@AlphaChars @StringLength(min = 1, max = 10) String> paramNames,
            @ForAll @Size(min = 1, max = 3) List<@AlphaChars @StringLength(min = 1, max = 15) String> paramValues) {
        
        Assume.that(paramNames.size() == paramValues.size());
        Assume.that(new HashSet<>(paramNames).size() == paramNames.size()); // unique names
        
        // Build pattern like /users/:id/posts/:postId
        StringBuilder patternBuilder = new StringBuilder();
        for (String name : paramNames) {
            patternBuilder.append("/").append(":").append(name);
        }
        String pattern = patternBuilder.toString();
        
        // Build path like /users/123/posts/456
        StringBuilder pathBuilder = new StringBuilder();
        for (String value : paramValues) {
            pathBuilder.append("/").append(value);
        }
        String path = pathBuilder.toString();
        
        RouterPlugin router = new RouterPlugin();
        router.get(pattern, ctx -> {});
        
        RouterPlugin.RouteMatch match = router.match("GET", path);
        
        assertNotNull(match, "Route should match path");
        
        for (int i = 0; i < paramNames.size(); i++) {
            assertEquals(paramValues.get(i), match.params.get(paramNames.get(i)),
                "Parameter " + paramNames.get(i) + " should be extracted correctly");
        }
    }
    
    /**
     * **Feature: lite-java-framework, Property 4: Route Matching and Method Dispatch**
     * **Validates: Requirements 3.1, 3.3**
     * 
     * For any set of registered routes and any incoming request,
     * the framework should invoke exactly the handler whose path pattern and HTTP method match.
     */
    @Property(tries = 100)
    void routeMatchingAndMethodDispatch(
            @ForAll("httpMethods") String method,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String pathSegment) {
        
        RouterPlugin router = new RouterPlugin();
        
        router.route(method, "/" + pathSegment, ctx -> {});
        
        RouterPlugin.RouteMatch matched = router.match(method, "/" + pathSegment);
        
        assertNotNull(matched, "Should find matching route");
        assertNotNull(matched.handler, "Handler should not be null");
    }
    
    @Property(tries = 100)
    void differentMethodsDoNotMatch(
            @ForAll("httpMethods") String method1,
            @ForAll("httpMethods") String method2,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String path) {
        
        Assume.that(!method1.equalsIgnoreCase(method2));
        
        RouterPlugin router = new RouterPlugin();
        router.route(method1, "/" + path, ctx -> {});
        
        RouterPlugin.RouteMatch matched = router.match(method2, "/" + path);
        
        assertNull(matched, "Different HTTP method should not match");
    }
    
    @Provide
    Arbitrary<String> httpMethods() {
        return Arbitraries.of("GET", "POST", "PUT", "DELETE", "PATCH");
    }

    /**
     * **Feature: lite-java-framework, Property 6: Route Group Prefix Application**
     * **Validates: Requirements 3.5**
     * 
     * For any route group with prefix P and routes with paths [R1, R2, ...],
     * the effective paths should be [P+R1, P+R2, ...].
     */
    @Property(tries = 100)
    void routeGroupPrefixApplication(
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String prefix,
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String path) {
        
        RouterPlugin router = new RouterPlugin();
        RouterPlugin group = router.group("/" + prefix);
        
        group.get("/" + path, ctx -> {});
        
        String expectedPath = "/" + prefix + "/" + path;
        RouterPlugin.RouteMatch matched = router.match("GET", expectedPath);
        
        assertNotNull(matched, "Route should be accessible with group prefix");
    }
    
    /**
     * **Feature: lite-java-framework, Property 7: Unmatched Route Returns 404**
     * **Validates: Requirements 3.4**
     * 
     * For any request path that does not match any registered route,
     * the router should return null (indicating 404).
     */
    @Property(tries = 100)
    void unmatchedRouteReturnsNull(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String registeredPath,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String requestPath) {
        
        Assume.that(!registeredPath.equals(requestPath));
        
        RouterPlugin router = new RouterPlugin();
        router.get("/" + registeredPath, ctx -> {});
        
        RouterPlugin.RouteMatch matched = router.match("GET", "/" + requestPath);
        
        assertNull(matched, "Non-matching path should return null");
    }
    
    @Property(tries = 100)
    void emptyRouterReturnsNull(
            @ForAll("httpMethods") String method,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String path) {
        
        RouterPlugin router = new RouterPlugin();
        
        RouterPlugin.RouteMatch matched = router.match(method, "/" + path);
        
        assertNull(matched, "Empty router should return null for any path");
    }
    
    /**
     * Test that routes with parameters match correctly.
     */
    @Property(tries = 100)
    void parameterizedRouteMatching(
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String paramName,
            @ForAll @AlphaChars @StringLength(min = 1, max = 15) String paramValue) {
        
        RouterPlugin router = new RouterPlugin();
        router.get("/users/:" + paramName, ctx -> {});
        
        RouterPlugin.RouteMatch matched = router.match("GET", "/users/" + paramValue);
        
        assertNotNull(matched, "Parameterized route should match");
        assertEquals(paramValue, matched.params.get(paramName));
    }
}
