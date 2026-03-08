# ── Build stage ──────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY filesystem-api/pom.xml filesystem-api/pom.xml
COPY filesystem-api-browserless/pom.xml filesystem-api-browserless/pom.xml
COPY demo/pom.xml demo/pom.xml

# Download dependencies (cached layer)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -ntp -B -q || true

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -Pdemo -DskipTests -pl demo -am -ntp -B

# ── Runtime stage ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/demo/target/filesystem-api-demo-1.0-SNAPSHOT.jar app.jar

ENV PORT=8080

CMD ["java", "-jar", "app.jar"]
