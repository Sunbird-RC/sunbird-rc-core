IMAGE:=dockerhub/sunbird-rc-credentials-service

.PHONY: docker publish test

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)
test:
	@docker-compose -f docker-compose-test.yml down
	@docker-compose  -f docker-compose-test.yml up --build --abort-on-container-exit