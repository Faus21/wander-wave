FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/wanderwave-0.0.1-SNAPSHOT.jar /app/wanderwave.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/wanderwave.jar"]
