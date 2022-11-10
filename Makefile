#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
RELEASE_VERSION = v0.0.10
build: java/registry/target/registry.jar
	echo ${SOURCES}
	rm -rf java/claim/target/*.jar
	cd target && rm -rf * && jar xvf ../java/registry/target/registry.jar && cp ../java/Dockerfile ./ && docker build -t dockerhub/sunbird-rc-core .
	make -C java/claim
	make -C services/certificate-api docker
	make -C services/certificate-signer docker
	make -C services/notification-service docker
	make -C deps/keycloak build
	make -C services/public-key-service docker
	make -C services/context-proxy-service docker
	docker build -t dockerhub/sunbird-rc-nginx .

java/registry/target/registry.jar: $(SOURCES)
	echo $(SOURCES)
	sh configure-dependencies.sh
	cd java && ./mvnw clean install

test: build
	@RELEASE_VERSION=latest docker-compose up -d
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@docker-compose logs
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw -Pe2e test || echo 'Tests failed'
	@docker-compose down
	make -C services/certificate-signer test
	make -C services/public-key-service test
	make -C services/context-proxy-service test

clean:
	@rm -rf target || true
	@rm java/registry/target/registry.jar || true
release: test
	docker tag dockerhub/sunbird-rc-core dockerhub/sunbird-rc-core:$(RELEASE_VERSION)
	docker tag dockerhub/sunbird-rc-claim-ms dockerhub/sunbird-rc-claim-ms:$(RELEASE_VERSION)
	docker tag dockerhub/sunbird-rc-notification-service dockerhub/sunbird-rc-notification-service:$(RELEASE_VERSION)
	docker tag dockerhub/sunbird-rc-certificate-signer dockerhub/sunbird-rc-certificate-signer:$(RELEASE_VERSION)
	docker tag dockerhub/sunbird-rc-certificate-api dockerhub/sunbird-rc-certificate-api:$(RELEASE_VERSION)
	docker tag dockerhub/sunbird-rc-keycloak dockerhub/sunbird-rc-keycloak:$(RELEASE_VERSION)
	docker tag dockerhub/sunbird-rc-public-key-service dockerhub/sunbird-rc-public-key-service:$(RELEASE_VERSION)
	docker tag dockerhub/sunbird-rc-context-proxy-service dockerhub/sunbird-rc-context-proxy-service:$(RELEASE_VERSION)
	docker tag dockerhub/sunbird-rc-nginx dockerhub/sunbird-rc-nginx:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-core:latest
	docker push dockerhub/sunbird-rc-core:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-claim-ms:latest 
	docker push dockerhub/sunbird-rc-claim-ms:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-notification-service:latest 
	docker push dockerhub/sunbird-rc-notification-service:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-certificate-signer:latest 
	docker push dockerhub/sunbird-rc-certificate-signer:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-certificate-api:latest 
	docker push dockerhub/sunbird-rc-certificate-api:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-keycloak:latest
	docker push dockerhub/sunbird-rc-keycloak:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-public-key-service:latest
	docker push dockerhub/sunbird-rc-public-key-service:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-context-proxy-service:latest
	docker push dockerhub/sunbird-rc-context-proxy-service:$(RELEASE_VERSION)
	docker push dockerhub/sunbird-rc-nginx:latest
	docker push dockerhub/sunbird-rc-nginx:$(RELEASE_VERSION)

