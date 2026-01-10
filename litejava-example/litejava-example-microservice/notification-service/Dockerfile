# LiteJava Notification Service
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY target/notification-service-*.jar app.jar
COPY docker-application.yml /app/application.yml

EXPOSE 8086

CMD ["java", "-jar", "app.jar"]
