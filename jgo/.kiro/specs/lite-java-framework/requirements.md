# Requirements Document

## Introduction

本项目旨在开发一个全新的Java Web框架，基于JDK 1.8，借鉴Koa 3.0的中间件设计和Go语言的简洁直接风格。框架核心理念是：拒绝注解、拒绝getter/setter、拒绝接口定义，追求代码的直观性和开箱即用体验。框架采用Maven构建，支持轻松集成数据库、缓存、WebSocket、SSL等常用组件。

## Glossary

- **LiteJava**: 本框架的名称，代表轻量级、直接的Java开发方式
- **Middleware（中间件）**: 借鉴Koa的洋葱模型，处理请求的可组合函数链
- **Context（上下文）**: 封装HTTP请求和响应的统一对象，贯穿整个请求生命周期
- **Handler（处理器）**: 处理具体业务逻辑的函数式接口
- **Router（路由器）**: 将URL路径映射到对应Handler的组件
- **Plugin（插件）**: 可插拔的功能扩展模块，如数据库、缓存等

## Requirements

### Requirement 1: 应用启动与配置

**User Story:** As a developer, I want to start a web server with minimal code, so that I can quickly begin development without complex configuration.

#### Acceptance Criteria

1. WHEN a developer creates a new LiteJava application THEN the LiteJava framework SHALL provide a fluent API to configure and start the server in less than 5 lines of code
2. WHEN a developer specifies a port number THEN the LiteJava framework SHALL bind the HTTP server to that port
3. WHEN a developer does not specify a port number THEN the LiteJava framework SHALL use port 8080 as the default
4. WHEN the server starts successfully THEN the LiteJava framework SHALL log the startup message with the bound port
5. IF the specified port is already in use THEN the LiteJava framework SHALL throw a clear exception with the port conflict information

### Requirement 2: 中间件系统

**User Story:** As a developer, I want to use a middleware system like Koa, so that I can compose request processing logic in a clean and reusable way.

#### Acceptance Criteria

1. WHEN a developer registers a middleware THEN the LiteJava framework SHALL execute middlewares in the order they were registered
2. WHEN a middleware calls the next function THEN the LiteJava framework SHALL pass control to the next middleware in the chain
3. WHEN a middleware completes after calling next THEN the LiteJava framework SHALL allow the middleware to execute post-processing logic (onion model)
4. WHEN a middleware does not call next THEN the LiteJava framework SHALL stop the middleware chain execution
5. WHEN an exception occurs in a middleware THEN the LiteJava framework SHALL propagate the exception to the error handling middleware

### Requirement 3: 路由系统

**User Story:** As a developer, I want to define routes with a simple and direct syntax, so that I can map URLs to handlers without annotations or XML configuration.

#### Acceptance Criteria

1. WHEN a developer defines a route with a path and HTTP method THEN the LiteJava framework SHALL register the route and invoke the corresponding handler on matching requests
2. WHEN a developer defines a route with path parameters (e.g., /users/:id) THEN the LiteJava framework SHALL extract and provide the parameter values in the context
3. WHEN a developer defines multiple routes with the same path but different HTTP methods THEN the LiteJava framework SHALL dispatch to the correct handler based on the request method
4. WHEN a request matches no defined route THEN the LiteJava framework SHALL return a 404 response with a clear message
5. WHEN a developer groups routes with a common prefix THEN the LiteJava framework SHALL support route grouping without requiring inheritance or interfaces

### Requirement 4: Context对象

**User Story:** As a developer, I want a unified context object for request and response, so that I can access all request data and control the response in one place.

#### Acceptance Criteria

1. WHEN a request arrives THEN the LiteJava framework SHALL create a Context object containing request method, path, headers, query parameters, and body
2. WHEN a developer accesses request body THEN the LiteJava framework SHALL provide methods to read body as string, bytes, or parsed JSON (as Map or List)
3. WHEN a developer sets response data THEN the LiteJava framework SHALL provide fluent methods to set status code, headers, and body
4. WHEN a developer sets response body as an object THEN the LiteJava framework SHALL automatically serialize the object to JSON
5. WHEN a developer accesses path parameters THEN the LiteJava framework SHALL provide a simple method to retrieve parameter values by name

### Requirement 5: JSON处理

**User Story:** As a developer, I want built-in JSON support without external configuration, so that I can easily work with JSON data in requests and responses.

#### Acceptance Criteria

1. WHEN a developer reads JSON from request body THEN the LiteJava framework SHALL parse JSON into Map, List, or primitive types without requiring custom classes
2. WHEN a developer writes an object to response THEN the LiteJava framework SHALL serialize Map, List, and primitive types to JSON automatically
3. WHEN parsing invalid JSON THEN the LiteJava framework SHALL throw a clear exception with the parsing error details
4. WHEN serializing to JSON THEN the LiteJava framework SHALL handle nested structures (Map containing List, etc.) correctly
5. WHEN a developer needs custom JSON handling THEN the LiteJava framework SHALL allow replacing the default JSON processor with a custom implementation

### Requirement 6: 插件系统

**User Story:** As a developer, I want to easily integrate common components like database and cache, so that I can extend the framework without complex configuration.

#### Acceptance Criteria

1. WHEN a developer registers a plugin THEN the LiteJava framework SHALL initialize the plugin and make its functionality available through the context
2. WHEN a developer accesses a registered plugin THEN the LiteJava framework SHALL provide the plugin instance through a simple context method
3. WHEN a plugin requires configuration THEN the LiteJava framework SHALL accept configuration as a simple Map or Properties object
4. WHEN a plugin fails to initialize THEN the LiteJava framework SHALL throw a clear exception with the failure reason
5. WHEN multiple plugins are registered THEN the LiteJava framework SHALL initialize them in registration order and allow inter-plugin dependencies

### Requirement 7: 数据库集成

**User Story:** As a developer, I want simple database access without ORM complexity, so that I can execute SQL queries and get results directly.

#### Acceptance Criteria

1. WHEN a developer configures a database connection THEN the LiteJava framework SHALL establish a connection pool with the provided settings
2. WHEN a developer executes a SELECT query THEN the LiteJava framework SHALL return results as a List of Map objects (column name to value)
3. WHEN a developer executes an INSERT, UPDATE, or DELETE query THEN the LiteJava framework SHALL return the number of affected rows
4. WHEN a developer uses parameterized queries THEN the LiteJava framework SHALL safely bind parameters to prevent SQL injection
5. WHEN a database error occurs THEN the LiteJava framework SHALL wrap the error in a framework exception with clear context information

### Requirement 8: 缓存集成

**User Story:** As a developer, I want simple cache operations, so that I can store and retrieve data without complex cache configuration.

#### Acceptance Criteria

1. WHEN a developer configures a cache plugin THEN the LiteJava framework SHALL support both in-memory cache and Redis as backends
2. WHEN a developer stores a value in cache THEN the LiteJava framework SHALL accept a key, value, and optional TTL (time-to-live)
3. WHEN a developer retrieves a value from cache THEN the LiteJava framework SHALL return the value or null if not found or expired
4. WHEN a developer deletes a cache entry THEN the LiteJava framework SHALL remove the entry and return success status
5. WHEN cache operations fail THEN the LiteJava framework SHALL throw a clear exception with the failure details

### Requirement 9: WebSocket支持

**User Story:** As a developer, I want to handle WebSocket connections easily, so that I can build real-time features without additional libraries.

#### Acceptance Criteria

1. WHEN a developer defines a WebSocket endpoint THEN the LiteJava framework SHALL handle the WebSocket upgrade handshake automatically
2. WHEN a WebSocket message is received THEN the LiteJava framework SHALL invoke the registered message handler with the message content
3. WHEN a developer sends a WebSocket message THEN the LiteJava framework SHALL transmit the message to the connected client
4. WHEN a WebSocket connection opens or closes THEN the LiteJava framework SHALL invoke the corresponding lifecycle handlers
5. WHEN a WebSocket error occurs THEN the LiteJava framework SHALL invoke the error handler with the exception details

### Requirement 10: SSL/HTTPS支持

**User Story:** As a developer, I want to enable HTTPS easily, so that I can secure my application without complex SSL configuration.

#### Acceptance Criteria

1. WHEN a developer provides SSL certificate and key files THEN the LiteJava framework SHALL configure HTTPS on the specified port
2. WHEN a developer enables SSL THEN the LiteJava framework SHALL support both HTTP and HTTPS on different ports simultaneously
3. WHEN SSL configuration is invalid THEN the LiteJava framework SHALL throw a clear exception with the configuration error
4. WHEN a developer does not configure SSL THEN the LiteJava framework SHALL run in HTTP-only mode without errors

### Requirement 11: 错误处理

**User Story:** As a developer, I want clear and consistent error handling, so that I can debug issues quickly and provide meaningful error responses.

#### Acceptance Criteria

1. WHEN an unhandled exception occurs THEN the LiteJava framework SHALL return a 500 response with error details in development mode
2. WHEN an unhandled exception occurs in production mode THEN the LiteJava framework SHALL return a generic 500 response without exposing internal details
3. WHEN a developer registers a custom error handler THEN the LiteJava framework SHALL invoke the custom handler for all unhandled exceptions
4. WHEN an error occurs THEN the LiteJava framework SHALL log the error with stack trace and request context

### Requirement 12: 静态文件服务

**User Story:** As a developer, I want to serve static files easily, so that I can host frontend assets without additional configuration.

#### Acceptance Criteria

1. WHEN a developer configures a static file directory THEN the LiteJava framework SHALL serve files from that directory for matching URL paths
2. WHEN a static file is requested THEN the LiteJava framework SHALL set appropriate Content-Type headers based on file extension
3. WHEN a requested static file does not exist THEN the LiteJava framework SHALL return a 404 response
4. WHEN serving static files THEN the LiteJava framework SHALL support common file types including HTML, CSS, JavaScript, images, and fonts

### Requirement 13: 请求体解析

**User Story:** As a developer, I want automatic request body parsing, so that I can access form data and JSON without manual parsing.

#### Acceptance Criteria

1. WHEN a request has Content-Type application/json THEN the LiteJava framework SHALL parse the body as JSON automatically
2. WHEN a request has Content-Type application/x-www-form-urlencoded THEN the LiteJava framework SHALL parse the body as form data
3. WHEN a request has Content-Type multipart/form-data THEN the LiteJava framework SHALL parse the body and provide access to uploaded files
4. WHEN body parsing fails THEN the LiteJava framework SHALL throw a clear exception with the parsing error details

### Requirement 14: 统一配置文件系统

**User Story:** As a developer, I want a unified configuration file system, so that I can manage all application settings in one place without scattered configuration.

#### Acceptance Criteria

1. WHEN a developer creates a configuration file THEN the LiteJava framework SHALL support YAML, JSON, and Properties formats
2. WHEN a developer loads configuration THEN the LiteJava framework SHALL merge configurations from multiple sources with clear precedence (file < environment variables < programmatic)
3. WHEN a developer accesses configuration values THEN the LiteJava framework SHALL provide type-safe access methods for String, Integer, Boolean, List, and Map types
4. WHEN a configuration key is missing THEN the LiteJava framework SHALL return a specified default value or throw a clear exception if required
5. WHEN environment-specific configuration is needed THEN the LiteJava framework SHALL support profile-based configuration files (e.g., config-dev.yml, config-prod.yml)
6. WHEN a plugin requires configuration THEN the LiteJava framework SHALL allow plugins to read their configuration from the unified config system using a namespace prefix
7. WHEN configuration values contain placeholders (e.g., ${DB_HOST}) THEN the LiteJava framework SHALL resolve placeholders from environment variables
8. WHEN configuration is loaded THEN the LiteJava framework SHALL validate required fields and report all missing required configurations at startup
