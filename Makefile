

build: java
	cd target && jar xvf ../java/registry/target/registry.jar && cp ../Dockerfile ./ && docker build .

java: java/registry/target/registry.jar
	cd java && ./mvnw -DskipTests clean install
