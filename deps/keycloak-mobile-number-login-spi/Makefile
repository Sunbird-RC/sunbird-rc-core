

.PHONY:
build: target/keycloak-mobile-number-login-spi-1.0-SNAPSHOT.jar
	echo "Building .."
	@./mvnw clean install
	@sh -c "mkdir -p ../keycloak/providers && cp target/keycloak-mobile-number-login-spi-1.0-SNAPSHOT.jar ../keycloak/providers"
