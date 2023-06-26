IMAGE:=dockerhub/sunbird-rc-identity-service

.PHONY: docker publish test run unseal

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)
test:
	@docker build -f Dockerfile.test -t $(IMAGE)-test .  
	@docker run $(IMAGE)-test
compose-init:
	bash ./build/setup_vault.sh