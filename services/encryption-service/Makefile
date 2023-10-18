IMAGE_NAME=ghcr.io/sunbird-rc/encryption-service

build:
	rm -rf ./target
	mvn clean install

docker: build
	docker build -t $(IMAGE_NAME) .

release: docker
    @docker push $(IMAGE_NAME):latest

