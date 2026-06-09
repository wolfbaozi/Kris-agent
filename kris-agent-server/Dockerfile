FROM docker.m.daocloud.io/eclipse-temurin:8-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 3001

ENTRYPOINT ["java", "-jar", "app.jar"]
