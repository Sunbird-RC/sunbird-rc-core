FROM frolvlad/alpine-java:jdk8-slim
WORKDIR /app
ADD ./target/egov-enc-service-1.1.4.jar enc-service.jar
EXPOSE 8013
ENTRYPOINT ["java", "-jar", "enc-service.jar"]
