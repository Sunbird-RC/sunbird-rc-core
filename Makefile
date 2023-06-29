#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
RELEASE_VERSION = v0.0.13
IMAGES := dockerhub/sunbird-rc-core dockerhub/sunbird-rc-nginx dockerhub/sunbird-rc-context-proxy-service \
			dockerhub/sunbird-rc-public-key-service dockerhub/sunbird-rc-keycloak dockerhub/sunbird-rc-certificate-api \
			dockerhub/sunbird-rc-certificate-signer dockerhub/sunbird-rc-notification-service dockerhub/sunbird-rc-claim-ms \
			dockerhub/sunbird-rc-digilocker-certificate-api dockerhub/sunbird-rc-bulk-issuance
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
	make -C services/metrics docker
	make -C services/digilocker-certificate-api docker
	make -C services/bulk_issuance docker
	docker build -t dockerhub/sunbird-rc-nginx .

java/registry/target/registry.jar: $(SOURCES)
	echo $(SOURCES)
	sh configure-dependencies.sh
	cd java && ./mvnw clean install

test: build
	@docker-compose down
	@rm -rf db-data* || echo "no permission to delete"
	# test with ES & standard definition manager
	@RELEASE_VERSION=latest KEYCLOAK_IMPORT_DIR=java/apitest/src/test/resources KEYCLOAK_SECRET=a52c5f4a-89fd-40b9-aea2-3f711f14c889 DB_DIR=db-data-1 docker-compose up -d db es keycloak registry certificate-signer certificate-api
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw -Pe2e test || echo 'Tests failed'
	@docker-compose down
	@rm -rf db-data-1 || echo "no permission to delete"
	# test with distributed definition manager
	@RELEASE_VERSION=latest KEYCLOAK_IMPORT_DIR=java/apitest/src/test/resources KEYCLOAK_SECRET=a52c5f4a-89fd-40b9-aea2-3f711f14c889 MANAGER_TYPE=DistributedDefinitionsManager DB_DIR=db-data-2 docker-compose up -d db es keycloak registry certificate-signer certificate-api redis
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw -Pe2e test || echo 'Tests failed'
	@docker-compose down
	@rm -rf db-data-2 || echo "no permission to delete"
	# test with native search service
	@RELEASE_VERSION=latest KEYCLOAK_IMPORT_DIR=java/apitest/src/test/resources KEYCLOAK_SECRET=a52c5f4a-89fd-40b9-aea2-3f711f14c889 DB_DIR=db-data-3 SEARCH_PROVIDER_NAME=dev.sunbirdrc.registry.service.NativeSearchService docker-compose up -d db keycloak registry certificate-signer certificate-api
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw -Pe2e test || echo 'Tests failed'
	@docker-compose down
	@rm -rf db-data-3 || echo "no permission to delete"
	# test with kafka(async)
	@ASYNC_ENABLED=true RELEASE_VERSION=latest KEYCLOAK_IMPORT_DIR=java/apitest/src/test/resources KEYCLOAK_SECRET=a52c5f4a-89fd-40b9-aea2-3f711f14c889 DB_DIR=db-data-4 docker-compose up -d db es keycloak registry certificate-signer certificate-api kafka zookeeper
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && MODE=async ../mvnw -Pe2e test || echo 'Tests failed'
	@docker-compose down
	@rm -rf db-data-4 || echo "no permission to delete"
	@RELEASE_VERSION=latest KEYCLOAK_IMPORT_DIR=java/apitest/src/test/resources EVENT_ENABLED=true KEYCLOAK_SECRET=a52c5f4a-89fd-40b9-aea2-3f711f14c889 DB_DIR=db-data-5 SEARCH_PROVIDER_NAME=dev.sunbirdrc.registry.service.NativeSearchService docker-compose up -d db keycloak kafka zookeeper keycloak clickhouse registry certificate-signer certificate-api metrics redis
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && MODE=events ../mvnw -Pe2e test || echo 'Tests failed'
	@docker-compose down
	@rm -rf db-data-5 || echo "no permission to delete"
	@NOTIFICATION_URL=http://notification-ms:8765/notification-service/v1/notification TRACK_NOTIFICATIONS=true KAFKA_BOOTSTRAP_SERVERS=kafka:9092 NOTIFICATION_ENABLED=true NOTIFICATION_ASYNC_ENABLED=false RELEASE_VERSION=latest KEYCLOAK_IMPORT_DIR=java/apitest/src/test/resources KEYCLOAK_SECRET=a52c5f4a-89fd-40b9-aea2-3f711f14c889 DB_DIR=db-data-6 docker-compose up -d db es keycloak registry certificate-signer certificate-api notification-ms kafka
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && MODE=notification ../mvnw -Pe2e test || echo 'Tests failed'
	@docker-compose down
	@rm -rf db-data-6 || echo "no permission to delete"
	make -C services/certificate-signer test
	make -C services/public-key-service test
	make -C services/context-proxy-service test
	make -C services/bulk_issuance test

clean:
	@rm -rf target || true
	@rm java/registry/target/registry.jar || true
release: test
	for image in $(IMAGES); \
    	do \
    	  echo $$image; \
    	  docker tag $$image:latest $$image:$(RELEASE_VERSION);\
    	  docker push $$image:latest;\
    	  docker push $$image:$(RELEASE_VERSION);\
      	done
	@cd tools/cli/ && npm publish
