FROM frolvlad/alpine-java:jdk8-slim
#COPY ./java/registry/target/registry.jar /home/opensaber/registry.jar
COPY ./BOOT-INF/lib /home/opensaber/BOOT-INF/lib
COPY ./META-INF  /home/opensaber/META-INF
COPY ./org  /home/opensaber/org
COPY ./BOOT-INF/classes /home/opensaber/BOOT-INF/classes
RUN mkdir -p /home/opensaber/config/public/_schemas
CMD ["java", "-Xms1024m", "-Xmx2048m", "-XX:+UseG1GC", "-Dserver.port=8081", "-cp", "/home/opensaber/config:/home/opensaber", "org.springframework.boot.loader.JarLauncher"]
