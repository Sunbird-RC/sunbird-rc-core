
IMAGE:=dockerhub/sunbird-rc-certificate-signer

docker:
	@cp config/keys.js.sample config/keys.js
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)