
IMAGE:=dockerhub/sunbird-rc-certificate-signer

test: docker
	@docker-compose -f api-test-docker-compose.yml up -d
	@docker-compose -f api-test-docker-compose.yml run test-runner
	#@papermill --log-output certificate-signer.ipynb out
	@docker-compose down
docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)