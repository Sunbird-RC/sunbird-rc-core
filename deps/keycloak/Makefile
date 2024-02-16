IMAGE_DESTINATION=ghcr.io/sunbird-rc/sunbird-rc-keycloak
RELEASE_VERSION=v1.0.1
build:
	echo "Building docker image"
	@cd ../keycloak-mobile-number-login-spi/ && sh ./build.sh
	@docker build -t $(IMAGE_DESTINATION) .

push:
	@docker tag $(IMAGE_DESTINATION) $(IMAGE_DESTINATION):$(RELEASE_VERSION)
	@docker push $(IMAGE_DESTINATION)
	@docker push $(IMAGE_DESTINATION):$(RELEASE_VERSION)

build-silicon-base:
	@docker build -t ghcr.io/sunbird-rc/keycloak:$(RELEASE_VERSION)-apple-silicon -f apple-silicon/Dockerfile apple-silicon

build-silicon:
	echo "Building docker image"
	@cd ../keycloak-mobile-number-login-spi/ && sh ./build.sh
	@docker build -t $(IMAGE_DESTINATION):$(RELEASE_VERSION)-apple-silicon --build-arg KEYCLOAK_BASE_IMAGE=ghcr.io/sunbird-rc/keycloak:1.0.0-apple-silicon .

push-silicon:
	@docker push $(IMAGE_DESTINATION):$(RELEASE_VERSION)-apple-silicon
  