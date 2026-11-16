# ---------- Stage 1: resolve deps + build (Maven + JDK 11) ----------
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build

# Copy ONLY the POM first — this layer caches all Maven Central downloads
COPY pom.xml .
RUN mvn -B -e dependency:go-offline

# Sources come after, so editing code doesn't re-download dependencies
RUN mvn -B package -DskipTests \
    && mvn -B dependency:copy-dependencies -DoutputDirectory=target/lib

# ---------- Stage 2: runtime (JDK 11) ----------
FROM eclipse-temurin:11-jdk-jammy

WORKDIR /app
COPY --from=build /build/target/lib /app/lib

# Start configuration server. By default, it will look for a file called config.db in the current directory.
# The server will be available at http://localhost:10101/get?id=
EXPOSE 10101
ENTRYPOINT [ \
    "java", \
    "-jar", "/app/lib/config-0.1.1-jar-with-dependencies.jar", \
    "-a", "run" \
]
CMD ["-d", "/app/config.db"]
