FROM gradle:8.14-jdk17 AS build

WORKDIR /workspace

COPY settings.gradle gradle.properties ./
COPY build.gradle.docker ./build.gradle
COPY src ./src

RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
