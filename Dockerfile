# Stage 1: build the jar with Maven (the image has JDK + Maven, nothing to install on your PC)
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app

# Download dependencies first, in their own layer, so they are cached
# and only re-downloaded when pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in IntelliJ / `mvn test`; skip them here to keep image builds fast.
RUN mvn -B -q package -DskipTests

# Stage 2: small runtime image with only the JRE and the jar
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as a normal user, not root
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/library-sdd-0.0.1-SNAPSHOT.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
