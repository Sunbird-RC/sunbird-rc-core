

build:
	echo "Building docker image"
	@cd ../keycloak-mobile-number-login-spi/ && sh ./build.sh
	@docker build -t dockerhub/ndear-keycloak .

push:
	@docker push dockerhub/ndear-keycloak
