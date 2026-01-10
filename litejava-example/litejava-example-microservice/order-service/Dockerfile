# LiteJava Order Service
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY target/order-service-*.jar app.jar
COPY docker-application.yml /app/application.yml

EXPOSE 8082

CMD ["java", "-jar", "app.jar"]
