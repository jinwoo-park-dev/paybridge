FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
