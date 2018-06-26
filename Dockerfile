FROM frolvlad/alpine-oraclejdk8:slim
COPY ./target/registry.jar /registry.jar
CMD java -jar -Xms1024m -Xmx2048m -XX:+UseG1GC -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=50M -Xloggc:logs/opensaber_gc.log /registry.jar

