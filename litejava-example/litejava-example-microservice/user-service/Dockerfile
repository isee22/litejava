# LiteJava User Service
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY target/user-service-*.jar app.jar
COPY docker-application.yml /app/application.yml

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]
