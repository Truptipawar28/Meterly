# ---- Stage 1: build the jar (has Maven + full JDK, thrown away afterwards) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only pom.xml first and download dependencies: this layer is cached
# and only re-runs when pom.xml changes, not on every code change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Stage 2: run it (small JRE-only image, no Maven, no source code) ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run as root inside the container
RUN groupadd --system meterly && useradd --system --gid meterly meterly
COPY --from=build /app/target/meterly-*.jar app.jar
USER meterly

EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
