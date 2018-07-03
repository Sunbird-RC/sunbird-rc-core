FROM frolvlad/alpine-oraclejdk8:slim
COPY ./java/registry/target/registry.jar /home/opensaber/registry.jar
<<<<<<< Updated upstream
CMD java -jar -Xms1024m -Xmx2048m -XX:+UseG1GC -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=50M -Xloggc:logs/opensaber_gc.log /registry.jar
=======
CMD ["java", "-Xms1024m", "-Xmx2048m", "-XX:+UseG1GC", "-jar", "/home/opensaber/registry.jar"]
>>>>>>> Stashed changes

