# Stage 1: Build
FROM gradle:8.12-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

RUN gradle buildFatJar --no-daemon -x test

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine AS run

WORKDIR /app

COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
