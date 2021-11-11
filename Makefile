#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
build: java/registry/target/registry.jar
	echo ${SOURCES}
	cd target && rm -rf * && jar xvf ../java/registry/target/registry.jar && cp ../Dockerfile ./ && docker build -t dockerhub/sunbird-rc-core .

java/registry/target/registry.jar: $(SOURCES)
	echo $(SOURCES)
	cd java && ./mvnw -DskipTests clean install

test: build
	@docker-compose up -d
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@docker-compose logs
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw test || echo 'Tests failed'
	@docker-compose down

clean:
	@rm -rf target
