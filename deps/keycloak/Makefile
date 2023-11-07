

build:
	echo "Building docker image"
	@cd ../keycloak-mobile-number-login-spi/ && sh ./build.sh
	@docker build -t ghcr.io/sunbird-rc/sunbird-rc-keycloak .

push:
	@docker push ghcr.io/sunbird-rc/sunbird-rc-keycloak
