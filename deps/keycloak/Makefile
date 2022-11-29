

build:
	echo "Building docker image"
	@cd ../keycloak-mobile-number-login-spi/ && sh ./build.sh
	@docker build -t dockerhub/sunbird-rc-keycloak .

push:
	@docker push dockerhub/sunbird-rc-keycloak
