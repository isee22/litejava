package example.resource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户 Resource - 使用 Jersey 完整 JAX-RS 特性
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {
    
    private static final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    private static final AtomicLong idGen = new AtomicLong(0);
    
    static {
        createUser("alice", "alice@example.com");
        createUser("bob", "bob@example.com");
    }
    
    @GET
    public Response list() {
        return Response.ok(Map.of("list", new ArrayList<>(users.values()))).build();
    }
    
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        Map<String, Object> user = users.get(id);
        if (user == null) {
            return Response.status(404).entity(Map.of("error", "User not found")).build();
        }
        return Response.ok(user).build();
    }
    
    @POST
    public Response create(Map<String, Object> body) {
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        if (name == null || name.isEmpty()) {
            return Response.status(400).entity(Map.of("error", "name is required")).build();
        }
        return Response.status(201).entity(createUser(name, email)).build();
    }
    
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, Map<String, Object> body) {
        Map<String, Object> user = users.get(id);
        if (user == null) {
            return Response.status(404).entity(Map.of("error", "User not found")).build();
        }
        if (body.containsKey("name")) user.put("name", body.get("name"));
        if (body.containsKey("email")) user.put("email", body.get("email"));
        return Response.ok(user).build();
    }
    
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        if (users.remove(id) == null) {
            return Response.status(404).entity(Map.of("error", "User not found")).build();
        }
        return Response.ok(Map.of("deleted", true)).build();
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
