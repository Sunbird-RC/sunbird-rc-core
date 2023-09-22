FROM frolvlad/alpine-java:jdk8-slim
COPY ./BOOT-INF/lib /home/sunbirdrc/BOOT-INF/lib
COPY ./META-INF  /home/sunbirdrc/META-INF
COPY ./org  /home/sunbirdrc/org
COPY ./BOOT-INF/classes /home/sunbirdrc/BOOT-INF/classes
CMD ["java", "-Xms1024m", "-Xmx2048m", "-XX:+UseG1GC", "-Dserver.port=8013", "-cp", "/home/sunbirdrc/config:/home/sunbirdrc", "org.springframework.boot.loader.JarLauncher"]
