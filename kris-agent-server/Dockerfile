FROM docker.m.daocloud.io/maven:3.9-eclipse-temurin-8 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM docker.m.daocloud.io/eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 3001
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
