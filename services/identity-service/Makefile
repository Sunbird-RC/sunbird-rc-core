IMAGE:=dockerhub/sunbird-rc-identity-service

.PHONY: docker publish test run unseal

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)
test:
	@docker-compose -f docker-compose-test.yml down
	bash build/setup_vault.sh docker-compose-test.yml vault-test
	@docker-compose -f docker-compose-test.yml up --build --abort-on-container-exit
compose-init:
	bash build/setup_vault.sh
	@docker-compose up -d --build
vault-reset:
	@docker volume rm identity-service_vault-data || echo ""
stop:
	@docker-compose stop