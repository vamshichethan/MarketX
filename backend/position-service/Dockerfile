FROM maven:3.9-eclipse-temurin-21 AS build

ARG SERVICE_DIR=position-service
WORKDIR /workspace

COPY common-events/pom.xml common-events/pom.xml
COPY common-events/src common-events/src
RUN mvn -f common-events/pom.xml clean install -DskipTests

COPY ${SERVICE_DIR}/pom.xml ${SERVICE_DIR}/pom.xml
COPY ${SERVICE_DIR}/src ${SERVICE_DIR}/src
RUN mvn -f ${SERVICE_DIR}/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/position-service/target/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh
EXPOSE 8081
ENTRYPOINT ["/app/docker-entrypoint.sh", "java", "-jar", "app.jar"]
