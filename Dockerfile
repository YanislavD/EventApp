# 1️⃣ Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build

COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -DskipITs

# 2️⃣ Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/EventApp-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
