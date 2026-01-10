FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY target/*-shaded.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
