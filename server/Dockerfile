FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system relax \
    && useradd --system --gid relax relax
COPY --from=build /workspace/target/relax-server-*.jar app.jar

USER relax
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
