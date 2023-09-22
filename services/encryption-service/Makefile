RELEASE_VERSION=v0.0.14
IMAGE_NAME=ghcr.io/sunbird-rc/encryption-service

build:
	rm -rf ./target
	mvn clean install -DSkipTests

build-image: build
	cd target && jar xvf egov-enc-service-1.1.4.jar && cp ../Dockerfile . && docker build -t $(IMAGE_NAME) .

release: build-image
	docker tag $(IMAGE_NAME):latest $(IMAGE_NAME):$(RELEASE_VERSION)
#    @docker push $(IMAGE_NAME):latest
#    @docker push $(IMAGE_NAME):$(RELEASE_VERSION)

