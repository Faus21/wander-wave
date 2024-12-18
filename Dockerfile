# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file to the container
COPY target/wanderwave-0.0.1-SNAPSHOT.jar /app/wanderwave.jar

# Expose the port your application will run on (update this if necessary)
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/app/wanderwave.jar"]
