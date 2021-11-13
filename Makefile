#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
build: build_modules build_registry build_claim
	echo "Registry and claim has built successfully! Escaped!"

build_modules: $(SOURCES)
	cd java && ./mvnw -DskipTests clean install

build_registry: $(SOURCES)
	cd java/registry/target && jar xvf registry.jar && cp ../Dockerfile ./ && docker build -t dockerhub/sunbird-rc-core .

build_claim: $(SOURCES)
	cd java/claim/target && jar xvf claim-0.0.1-SNAPSHOT.jar && cp ../Dockerfile ./ && docker build -t dockerhub/open-saber-claim-ms .

test: build
	@docker-compose up -d
	@echo "Starting the test" && bash build/wait_for_port.sh 8080 || (docker-compose logs && false)
	@echo "Starting the test" && bash build/wait_for_port.sh 8081 || (docker-compose logs && false)
	@docker-compose ps
	@docker-compose logs
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw test || echo 'Tests failed'
	@docker-compose down

clean:
	@rm -rf target
