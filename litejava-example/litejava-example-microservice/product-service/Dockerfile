# LiteJava Product Service
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY target/product-service-*.jar app.jar
COPY docker-application.yml /app/application.yml

EXPOSE 8083

CMD ["java", "-jar", "app.jar"]
