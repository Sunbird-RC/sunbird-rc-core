
IMAGE:=ghcr.io/sunbird-rc/sunbird-rc-context-proxy-service

test: docker
	@docker-compose -f api-test-docker-compose.yml up -d context-proxy-service
	@docker-compose -f api-test-docker-compose.yml run test-runner
	#@papermill --log-output certificate-signer.ipynb out
	@docker-compose -f api-test-docker-compose.yml down
docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)