# LiteJava Auth Service
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY target/auth-service-*.jar app.jar
COPY docker-application.yml /app/application.yml

EXPOSE 8085

CMD ["java", "-jar", "app.jar"]
