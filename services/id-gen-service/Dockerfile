FROM frolvlad/alpine-java:jdk8-slim
WORKDIR /app
ADD ./target/egov-idgen-1.2.4-SNAPSHOT.jar id-gen-service.jar
ENTRYPOINT ["java", "-jar", "id-gen-service.jar"]
