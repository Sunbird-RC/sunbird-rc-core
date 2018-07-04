FROM frolvlad/alpine-oraclejdk8:slim
COPY ./java/registry/target/registry.jar /home/opensaber/registry.jar
RUN mkdir /home/opensaber/config
CMD ["java", "-Xms1024m", "-Xmx2048m", "-XX:+UseG1GC", "-jar", "/home/opensaber/registry.jar"]
