FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /proyecto
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN adduser -D -u 10001 aplicacion
WORKDIR /app
COPY --from=build /proyecto/target/presupuestos-electricos-1.0.0.jar /app/api.jar
USER aplicacion
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "/app/api.jar"]
