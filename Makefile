#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
RELEASE_VERSION = v2.0.0-rc1
IMAGES := ghcr.io/sunbird-rc/sunbird-rc-core ghcr.io/sunbird-rc/sunbird-rc-claim-ms \
			ghcr.io/sunbird-rc/sunbird-rc-notification-service ghcr.io/sunbird-rc/sunbird-rc-metrics \
			ghcr.io/sunbird-rc/id-gen-service ghcr.io/sunbird-rc/encryption-service \
			ghcr.io/sunbird-rc/sunbird-rc-identity-service ghcr.io/sunbird-rc/sunbird-rc-credential-schema \
			ghcr.io/sunbird-rc/sunbird-rc-credentials-service
build: java/registry/target/registry.jar
	echo ${SOURCES}
	rm -rf java/claim/target/*.jar
	cd target && rm -rf * && jar xvf ../java/registry/target/registry.jar && cp ../java/Dockerfile ./ && docker build -t ghcr.io/sunbird-rc/sunbird-rc-core .
	make -C java/claim
	make -C services/notification-service docker
	make -C services/metrics docker
	make -C services/id-gen-service docker
	make -C services/encryption-service docker
	make -C services/identity-service/ docker
	make -C services/credential-schema docker
	make -C services/credentials-service/ docker


java/registry/target/registry.jar: $(SOURCES)
	echo $(SOURCES)
	sh configure-dependencies.sh
	cd java && ./mvnw clean install

test: build
	@docker-compose -f docker-compose-v1.yml down
	@rm -rf db-data* || echo "no permission to delete"
	@echo "storage check before starting the tests and after building the docker files"
	@df -h && du -h --max-depth=2 / && docker system df && docker image ls
	# test with distributed definition manager and native search
	@docker-compose -f docker-compose-v1.yml --env-file test_environments/test_with_distributedDefManager_nativeSearch.env up -d db keycloak registry certificate-signer certificate-api redis
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose -f docker-compose-v1.yml ps
	@echo "storage check after setting up services for 1nd test"
	@df -h && du -h --max-depth=2 / && docker system df && docker image ls
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw -Pe2e test
	@docker-compose -f docker-compose-v1.yml down
	@rm -rf db-data-1 || echo "no permission to delete"
	@echo "storage check after 1 e2e test completed"
	@df -h && du -h --max-depth=2 / && docker system df && docker image ls
	# test with kafka(async), events, notifications,
	@docker-compose -f docker-compose-v1.yml --env-file test_environments/test_with_asyncCreate_events_notifications.env up -d db es clickhouse redis keycloak registry certificate-signer certificate-api kafka zookeeper notification-ms metrics
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@docker-compose -f docker-compose-v1.yml ps
	@echo "storage check after setting up services for 2nd test"
	@df -h && du -h --max-depth=2 / && docker system df && docker image ls
	@curl -v http://localhost:8081/health
	@cd java/apitest && MODE=async ../mvnw -Pe2e test
	@docker-compose -f docker-compose-v1.yml down
	@rm -rf db-data-2 || echo "no permission to delete"
	@echo "storage check after completion of 2nd test"
	@df -h && du -h --max-depth=2 / && docker system df && docker image ls
#	make -C services/identity-service test
#	make -C services/credential-schema test
#	make -C services/credentials-service test

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

compose-init:
	bash setup_vault.sh docker-compose.yml vault
