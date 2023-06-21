IMAGE:=dockerhub/sunbird-rc-credential-schema

.PHONY: docker publish test

docker:
	@docker build -t $(IMAGE) .

publish:
	@docker push $(IMAGE)
	
test:
	@docker build -f Dockerfile.test -t $(IMAGE)-test .  
	@docker run $(IMAGE)-test