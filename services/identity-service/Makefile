IMAGE:=dockerhub/sunbird-rc-identity-service

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)