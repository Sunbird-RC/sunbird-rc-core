IMAGE:=ghcr.io/sunbird-rc/sunbird-rc-metrics
SPEC_FILE := interfaces/metrics-api.yaml
GOPATH := $(shell go env GOPATH)
source := $(wildcard *.go **/*.go */*/*.go)

all: metrics-server

deps:
	@GO111MODULE=off go get -u github.com/myitcv/gobin
	@$(GOPATH)/bin/gobin github.com/go-swagger/go-swagger/cmd/swagger@v0.25.0
	@$(GOPATH)/bin/gobin github.com/golangci/golangci-lint/cmd/golangci-lint@v1.24.0

swagger_gen/restapi/server.go: $(SPEC_FILE)
	@mkdir $(PWD)/swagger_gen || echo 'exists'
	@$(GOPATH)/bin/swagger generate server --exclude-main -t ./swagger_gen -f $(SPEC_FILE) --principal models.JWTClaimBody

metrics-server: $(source) swagger_gen/restapi/server.go
	@echo "Building Server to $(PWD)/metrics ..."
	@CGO_ENABLED=1 GO111MODULE=on go build $(OTHER_FLAGS) metrics

api_docs:
	@echo "Installing swagger-merger" && npm install swagger-merger -g
	@swagger-merger -i $(PWD)/interfaces/metrics-api.yaml -o $(PWD)/bundle.yaml

build: metrics-server
	@echo "building $(source)"
	@echo $(source)

docker:
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)