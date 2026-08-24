IMAGE:=ghcr.io/sunbird-rc/sunbird-rc-oid4vc-service
PLATFORM ?=
PLATFORM_FLAG := $(if $(PLATFORM),--platform $(PLATFORM),)
.PHONY: docker publish

docker:
	@docker build $(PLATFORM_FLAG) -t $(IMAGE) .
publish:
	@docker push $(IMAGE)
