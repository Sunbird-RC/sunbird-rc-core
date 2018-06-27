FROM frolvlad/alpine-oraclejdk8:slim
COPY ./java/registry/target/registry.jar /home/opensaber/registry.jar
COPY ./java/registry/src/main/resources/* /home/opensaber/config/
CMD ["java", "-cp", "/home/opensaber/config/*", "-Xms1024m", "-Xmx2048m", "-XX:+UseG1GC", "-jar", "/home/opensaber/registry.jar"]

