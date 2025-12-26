# Tech Stack

## Build System

- Maven multi-module project
- JDK 1.8 (Java 8)

## Project Modules

- `litejava-core` - Zero external dependencies, pure JDK implementation
- `litejava-plugins` - Optional official plugins with external dependencies

## Testing

- JUnit 5 for unit tests
- jqwik for property-based testing

## Key Dependencies

Core module: None (pure JDK)

Plugins module:
- JDBC drivers for database plugin
- Redis client for cache plugin

## Common Commands

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Run specific module tests
mvn test -pl litejava-core

# Package without tests
mvn package -DskipTests
```

## Code Style

- Use public fields instead of getters/setters
- Prefer `Map<String, Object>` over POJOs for data transfer
- Use functional interfaces and lambdas
- No annotations for configuration
- All framework exceptions extend `LiteJavaException`
