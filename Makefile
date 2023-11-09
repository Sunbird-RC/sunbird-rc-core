#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
RELEASE_VERSION = v0.0.14
IMAGES := ghcr.io/sunbird-rc/sunbird-rc-core ghcr.io/sunbird-rc/sunbird-rc-nginx ghcr.io/sunbird-rc/sunbird-rc-context-proxy-service \
			ghcr.io/sunbird-rc/sunbird-rc-public-key-service ghcr.io/sunbird-rc/sunbird-rc-keycloak ghcr.io/sunbird-rc/sunbird-rc-certificate-api \
			ghcr.io/sunbird-rc/sunbird-rc-certificate-signer ghcr.io/sunbird-rc/sunbird-rc-notification-service ghcr.io/sunbird-rc/sunbird-rc-claim-ms \
			ghcr.io/sunbird-rc/sunbird-rc-digilocker-certificate-api ghcr.io/sunbird-rc/sunbird-rc-bulk-issuance ghcr.io/sunbird-rc/sunbird-rc-metrics \
			ghcr.io/sunbird-rc/id-gen-service ghcr.io/sunbird-rc/encryption-service
build: java/registry/target/registry.jar
	echo ${SOURCES}
	rm -rf java/claim/target/*.jar
	cd target && rm -rf * && jar xvf ../java/registry/target/registry.jar && cp ../java/Dockerfile ./ && docker build -t ghcr.io/sunbird-rc/sunbird-rc-core .
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
	make -C services/id-gen-service docker
	make -C services/encryption-service docker
	docker build -t ghcr.io/sunbird-rc/sunbird-rc-nginx .

java/registry/target/registry.jar: $(SOURCES)
	echo $(SOURCES)
	sh configure-dependencies.sh
	cd java && ./mvnw clean install

test: build
	@docker-compose down
	@rm -rf db-data* || echo "no permission to delete"
	# test with distributed definition manager and native search
	@docker-compose --env-file test_environments/test_with_distributedDefManager_nativeSearch.env up -d db keycloak registry certificate-signer certificate-api redis
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw -Pe2e test
	@docker-compose down
	@rm -rf db-data-1 || echo "no permission to delete"
	# test with kafka(async), events, notifications,
	@docker-compose --env-file test_environments/test_with_asyncCreate_events_notifications.env up -d db clickhouse redis keycloak registry certificate-signer certificate-api kafka zookeeper notification-ms metrics
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && MODE=async ../mvnw -Pe2e test
	@docker-compose down
	@rm -rf db-data-2 || echo "no permission to delete"
	# test with fusionauth
	@docker-compose --env-file test_environments/test_with_fusionauth.env -f docker-compose.yml -f services/sample-fusionauth-service/docker-compose.yml up -d db es fusionauth fusionauthwrapper
	sleep 20
	@echo "Starting the test" && sh build/wait_for_port.sh 9011
	@echo "Starting the test" && sh build/wait_for_port.sh 3990
	sleep 20
	@docker-compose --env-file test_environments/test_with_fusionauth.env -f docker-compose.yml -f services/sample-fusionauth-service/docker-compose.yml up -d --no-deps registry
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose -f docker-compose.yml -f services/sample-fusionauth-service/docker-compose.yml ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && MODE=fusionauth ../mvnw -Pe2e test || echo 'Tests failed'
	@docker-compose -f docker-compose.yml -f services/sample-fusionauth-service/docker-compose.yml down
	@rm -rf db-data-3 || echo "no permission to delete"
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
