IMAGE_DESTINATION=ghcr.io/sunbird-rc

build:
	echo "Building docker image"
	@cd ../keycloak-mobile-number-login-spi/ && sh ./build.sh
	@docker build -t $(IMAGE_DESTINATION)/sunbird-rc-keycloak .

push:
	@docker push $(IMAGE_DESTINATION)/sunbird-rc-keycloak

build-silicon-base:
	@docker build -t $(IMAGE_DESTINATION)/keycloak-silicon -f apple-silicon/Dockerfile apple-silicon

build-silicon:
	echo "Building docker image"
	@cd ../keycloak-mobile-number-login-spi/ && sh ./build.sh
	@docker build -t $(IMAGE_DESTINATION)/sunbird-rc-keycloak:silicon --build-arg KEYCLOAK_BASE_IMAGE=vikastc/sunbirdrc-keycloak:apple-silicon .

push-silicon:
	@docker push $(IMAGE_DESTINATION)/sunbird-rc-keycloak:silicon
  