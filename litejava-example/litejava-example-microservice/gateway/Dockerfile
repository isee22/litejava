# LiteJava API Gateway
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY target/gateway-*.jar app.jar
COPY docker-application.yml /app/application.yml

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
