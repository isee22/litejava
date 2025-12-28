package example.controller;

import javax.ws.rs.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户 Controller - 使用 JAX-RS 标准注解
 */
@Path("/api/users")
public class UserController {
    
    private static final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    private static final AtomicLong idGen = new AtomicLong(0);
    
    static {
        createUser("alice", "alice@example.com");
        createUser("bob", "bob@example.com");
    }
    
    @GET
    public Map<String, Object> list() {
        return Map.of("list", new ArrayList<>(users.values()));
    }
    
    @GET
    @Path("/{id}")
    public Map<String, Object> get(@PathParam("id") long id) {
        Map<String, Object> user = users.get(id);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return user;
    }
    
    @POST
    public Map<String, Object> create(Map<String, Object> body) {
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        if (name == null || name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        return createUser(name, email);
    }
    
    @PUT
    @Path("/{id}")
    public Map<String, Object> update(@PathParam("id") long id, Map<String, Object> body) {
        Map<String, Object> user = users.get(id);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (body.containsKey("name")) user.put("name", body.get("name"));
        if (body.containsKey("email")) user.put("email", body.get("email"));
        return user;
    }
    
    @DELETE
    @Path("/{id}")
    public Map<String, Object> delete(@PathParam("id") long id) {
        if (users.remove(id) == null) {
            throw new NotFoundException("User not found");
        }
        return Map.of("deleted", true);
    }
    
    private static Map<String, Object> createUser(String name, String email) {
        long id = idGen.incrementAndGet();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("email", email);
        users.put(id, user);
        return user;
    }
}
