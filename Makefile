#SOURCES = $(wildcard java/**/*.java)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
SOURCES := $(call rwildcard,java/,*.java)
RELEASE_VERSION = v2.0.3
IMAGES := ghcr.io/sunbird-rc/sunbird-rc-core ghcr.io/sunbird-rc/sunbird-rc-claim-ms \
			ghcr.io/sunbird-rc/sunbird-rc-notification-service ghcr.io/sunbird-rc/sunbird-rc-metrics \
			ghcr.io/sunbird-rc/id-gen-service ghcr.io/sunbird-rc/encryption-service \
			ghcr.io/sunbird-rc/sunbird-rc-identity-service ghcr.io/sunbird-rc/sunbird-rc-credential-schema \
			ghcr.io/sunbird-rc/sunbird-rc-credentials-service ghcr.io/sunbird-rc/sunbird-rc-oid4vc-service
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
	make -C services/oid4vc-service/ docker


java/registry/target/registry.jar: $(SOURCES)
	echo $(SOURCES)
	sh configure-dependencies.sh
	cd java && ./mvnw clean install

test: build
	@docker-compose -f docker-compose-v1.yml down
	@sudo rm -rf db-data* es-data* || echo "no permission to delete"
	# test with distributed definition manager and native search
	@docker-compose -f docker-compose-v1.yml --env-file test_environments/test_with_distributedDefManager_nativeSearch.env up -d db keycloak registry certificate-signer certificate-api redis
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@echo "Waiting for Keycloak OIDC endpoint..." && curl --retry 30 --retry-delay 5 --retry-all-errors -sf -o /dev/null http://localhost:8080/auth/realms/sunbird-rc/.well-known/openid-configuration
	@docker-compose -f docker-compose-v1.yml ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && ../mvnw -Pe2e test
	@docker-compose -f docker-compose-v1.yml down
	@sudo rm -rf db-data-1 || echo "no permission to delete"
	# test with kafka(async), events, notifications,
	@docker-compose -f docker-compose-v1.yml --env-file test_environments/test_with_asyncCreate_events_notifications.env up -d db es clickhouse redis keycloak registry certificate-signer certificate-api kafka zookeeper notification-ms metrics
	@echo "Starting the test" && sh build/wait_for_port.sh 8080
	@echo "Starting the test" && sh build/wait_for_port.sh 8081
	@echo "Waiting for Keycloak OIDC endpoint..." && curl --retry 30 --retry-delay 5 --retry-all-errors -sf -o /dev/null http://localhost:8080/auth/realms/sunbird-rc/.well-known/openid-configuration
	@echo "Starting the test" && sh build/wait_for_port.sh 8070
	@docker-compose -f docker-compose-v1.yml ps
	@curl -v http://localhost:8081/health
	@cd java/apitest && MODE=async ../mvnw -Pe2e test
	@docker-compose -f docker-compose-v1.yml down
	@sudo rm -rf db-data-2 es-data-2 || echo "no permission to delete"

# V2 Node services: unit + DB/Vault-backed tests in each service's docker-compose-test.yml
test-services:
	make -C services/identity-service test
	make -C services/credential-schema test
	make -C services/credentials-service test

clean:
	@rm -rf target || true
	@rm java/registry/target/registry.jar || true

# Fails on fixable CRITICAL vulnerabilities in the built images. Accepted exceptions
# go in .trivyignore (see docs/conformance/EXCEPTIONS.md). CI overrides TRIVY to a pinned image.
# Scans the :latest images already on this machine, so run `make build` first
# (manual.yml and `make release` do). It has no build prerequisite because `build`
# always rebuilds, which would build every image twice in a release.
TRIVY ?= trivy
scan-images:
	@rc=0; for image in $(IMAGES); \
	do \
	  echo "Scanning $$image:latest"; \
	  $(TRIVY) image --scanners vuln --severity CRITICAL --ignore-unfixed --ignorefile .trivyignore --exit-code 1 $$image:latest || rc=1; \
	done; exit $$rc

# Pushes :$(RELEASE_VERSION), plus :latest only with PUSH_LATEST=true, so a patch release
# from an older branch cannot overwrite :latest by default. Same default as manual.yml's
# tag_latest input, which sets it.
PUSH_LATEST ?= false
# Tags every image before pushing any, so a missing image fails before anything is
# published. A push that fails midway can be re-run; pushing the same tags again is safe.
push-images:
	set -e; for image in $(IMAGES); do \
	  docker tag $$image:latest $$image:$(RELEASE_VERSION); \
	done; \
	for image in $(IMAGES); do \
	  echo $$image; \
	  if [ "$(PUSH_LATEST)" = "true" ]; then docker push $$image:latest; fi; \
	  docker push $$image:$(RELEASE_VERSION); \
	done

# registry-cli (formerly tools/cli) moved to github.com/Sunbird-RC/registry-cli in c521b666 and is released from there
# Steps run in order in the recipe (not as prerequisites), so `make -j release` cannot
# push before the tests and scan have passed.
release:
	$(MAKE) test
	$(MAKE) test-services
	$(MAKE) scan-images
	$(MAKE) push-images

compose-init:
	bash setup_vault.sh docker-compose.yml vault
