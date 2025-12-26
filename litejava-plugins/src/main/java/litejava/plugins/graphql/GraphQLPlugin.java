package litejava.plugins.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import litejava.Context;
import litejava.Plugin;

import java.util.Map;

/**
 * GraphQL 插件 - 基于 graphql-java
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.graphql-java</groupId>
 *     <artifactId>graphql-java</artifactId>
 *     <version>21.3</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * String schema = "type Query { user(id: ID!): User  users: [User] }\n" +
 *     "type User { id: ID!  name: String! }";
 * 
 * RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
 *     .type("Query", b -> b
 *         .dataFetcher("user", env -> userService.getById(env.getArgument("id")))
 *         .dataFetcher("users", env -> userService.list()))
 *     .build();
 * 
 * app.use(new GraphQLPlugin(schema, wiring));
 * }</pre>
 * 
 * @see <a href="https://www.graphql-java.com/documentation/getting-started">GraphQL Java Docs</a>
 */
public class GraphQLPlugin extends Plugin {
    
    public String path = "/graphql";
    public String graphiqlPath = "/graphiql";
    
    private GraphQL graphQL;
    private String schemaDefinition;
    private RuntimeWiring runtimeWiring;
    
    public GraphQLPlugin() {}
    
    public GraphQLPlugin(String schema, RuntimeWiring wiring) {
        this.schemaDefinition = schema;
        this.runtimeWiring = wiring;
    }
    
    public GraphQLPlugin schema(String schema) {
        this.schemaDefinition = schema;
        return this;
    }
    
    public GraphQLPlugin wiring(RuntimeWiring wiring) {
        this.runtimeWiring = wiring;
        return this;
    }
    
    @Override
    public void config() {
        if (schemaDefinition == null || runtimeWiring == null) {
            throw new IllegalStateException("GraphQL schema and wiring required");
        }
        
        TypeDefinitionRegistry registry = new SchemaParser().parse(schemaDefinition);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, runtimeWiring);
        graphQL = GraphQL.newGraphQL(schema).build();
        
        app.post(path, this::handleGraphQL);
        app.get(path, this::handleGraphQL);
        if (graphiqlPath != null) app.get(graphiqlPath, this::handleGraphiQL);
    }
    
    @SuppressWarnings("unchecked")
    private void handleGraphQL(Context ctx) {
        try {
            Map<String, Object> body;
            if ("GET".equals(ctx.method)) {
                body = Map.of("query", ctx.queryParam("query", ""),
                    "variables", ctx.queryParam("variables") != null 
                        ? ctx.app.json.parseMap(ctx.queryParam("variables")) : Map.of());
            } else {
                body = ctx.bindJSON();
            }
            
            String query = (String) body.get("query");
            if (query == null || query.isEmpty()) {
                ctx.status(400).json(Map.of("errors", new Object[]{Map.of("message", "Query required")}));
                return;
            }
            
            Map<String, Object> variables = (Map<String, Object>) body.getOrDefault("variables", Map.of());
            ExecutionResult result = graphQL.execute(b -> b.query(query).variables(variables)
                .operationName((String) body.get("operationName")));
            
            ctx.json(result.getErrors().isEmpty() 
                ? Map.of("data", result.getData())
                : Map.of("data", result.getData(), "errors", result.getErrors()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("errors", new Object[]{Map.of("message", e.getMessage())}));
        }
    }
    
    private void handleGraphiQL(Context ctx) {
        ctx.html("<!DOCTYPE html><html><head><title>GraphiQL</title>" +
            "<link href=\"https://unpkg.com/graphiql/graphiql.min.css\" rel=\"stylesheet\"/>" +
            "</head><body style=\"margin:0;\"><div id=\"graphiql\" style=\"height:100vh;\"></div>" +
            "<script src=\"https://unpkg.com/react/umd/react.production.min.js\"></script>" +
            "<script src=\"https://unpkg.com/react-dom/umd/react-dom.production.min.js\"></script>" +
            "<script src=\"https://unpkg.com/graphiql/graphiql.min.js\"></script>" +
            "<script>ReactDOM.render(React.createElement(GraphiQL,{fetcher:GraphiQL.createFetcher({url:'" + path + "'})}),document.getElementById('graphiql'));</script></body></html>");
    }
    
    public GraphQL getGraphQL() { return graphQL; }
}
