FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY java/target/registry-2.0.4-SNAPSHOT.jar registry.jar
RUN mkdir -p /home/sunbirdrc/config/public/_schemas
EXPOSE 8081
ENTRYPOINT ["java","-Xms1g", "-Xmx2g", "-jar", "registry.jar", "--server.port=8081"]

