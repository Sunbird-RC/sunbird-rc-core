


IMAGE:=dockerhub/sunbird-rc-id-generation

test:
	@mvn clean test

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)