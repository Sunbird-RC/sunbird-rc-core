FROM frolvlad/alpine-java:jdk8-slim
#COPY ./java/registry/target/registry.jar /home/sunbirdrc/registry.jar
COPY ./BOOT-INF/lib /home/sunbirdrc/BOOT-INF/lib
COPY ./META-INF  /home/sunbirdrc/META-INF
COPY ./org  /home/sunbirdrc/org
COPY ./BOOT-INF/classes /home/sunbirdrc/BOOT-INF/classes
COPY ./BOOT-INF/classes/application.yml.sample /home/sunbirdrc/BOOT-INF/classes/application.yml
COPY ./BOOT-INF/classes/frame.json.sample /home/sunbirdrc/BOOT-INF/classes/frame.json
RUN mkdir -p /home/sunbirdrc/config/public/_schemas
CMD ["java", "-Xms1024m", "-Xmx2048m", "-XX:+UseG1GC", "-Dserver.port=8081", "-cp", "/home/sunbirdrc/config:/home/sunbirdrc", "org.springframework.boot.loader.JarLauncher"]
