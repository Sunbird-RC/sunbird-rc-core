SPEC_FILE := ./digilocker_certificate.yml
GOPATH := $(shell go env GOPATH)
source := $(wildcard *.go **/*.go */*/*.go)
IMAGE := dockerhub/digilocker-certificate-api

all: digilocker_certificate_api

deps:
	@GO111MODULE=off go get -u github.com/myitcv/gobin
	@$(GOPATH)/bin/gobin github.com/go-swagger/go-swagger/cmd/swagger
	@$(GOPATH)/bin/gobin github.com/golangci/golangci-lint/cmd/golangci-lint@v1.24.0

swagger_gen/restapi/server.go: $(SPEC_FILE)
	@mkdir $(PWD)/swagger_gen || echo 'exists'
	@$(GOPATH)/bin/swagger generate server --exclude-main -t ./swagger_gen -f $(SPEC_FILE)

digilocker_certificate_api: $(source) swagger_gen/restapi/server.go
	@echo "Building Server to $(PWD)/digilocker_certificate_api ..."
	@CGO_ENABLED=1 GO111MODULE=on go build $(OTHER_FLAGS) digilocker_certificate_api

api_docs:
	@echo "Installing swagger-merger" && npm install swagger-merger -g
	@swagger-merger -i $(PWD)./digilocker_certificate.yaml -o $(PWD)/bundle.yaml

build: digilocker_certificate_api
	@echo "building $(source)"
	@echo $(source)

run: ./digilocker_certificate_api
	@echo "Running Digilocker Certificate Api Service"
	./digilocker_certificate_api --scheme http --port 8806

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)