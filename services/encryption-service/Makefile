RELEASE_VERSION=main
IMAGE_NAME=ghcr.io/sunbird-rc/encryption-service

build:
	rm -rf ./target
	mvn clean install

docker: build
	docker build -t $(IMAGE_NAME) .

release: docker
	docker tag $(IMAGE_NAME):latest $(IMAGE_NAME):$(RELEASE_VERSION)
    @docker push $(IMAGE_NAME):latest
    @docker push $(IMAGE_NAME):$(RELEASE_VERSION)

