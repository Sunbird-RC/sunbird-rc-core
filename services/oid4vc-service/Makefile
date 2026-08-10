IMAGE:=ghcr.io/sunbird-rc/sunbird-rc-oid4vc-service
.PHONY: docker publish

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)
