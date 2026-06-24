FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/demo-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Xms128m", "-Xmx400m", "-XX:MetaspaceSize=128m", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]