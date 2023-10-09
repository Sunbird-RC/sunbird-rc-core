SPEC_FILE := ./interfaces/bulk_issuance_api.yaml
GOPATH := $(shell go env GOPATH)
source := $(wildcard *.go **/*.go */*/*.go)
IMAGE := ghcr.io/sunbird-rc/sunbird-rc-bulk-issuance
all: bulk_issuance-server

deps:
	@GO111MODULE=off go get -u github.com/myitcv/gobin
	@$(GOPATH)/bin/gobin github.com/go-swagger/go-swagger/cmd/swagger@v0.25.0
	@$(GOPATH)/bin/gobin github.com/golangci/golangci-lint/cmd/golangci-lint@v1.24.0

swagger_gen/restapi/server.go: $(SPEC_FILE)
	@mkdir $(PWD)/swagger_gen || echo 'exists'
	@$(GOPATH)/bin/swagger generate server --exclude-main -t ./swagger_gen -f $(SPEC_FILE) --principal models.JWTClaimBody

bulk_issuance-server: $(source) swagger_gen/restapi/server.go
	@echo "Building Server to $(PWD)/bulk_issuance ..."
	@CGO_ENABLED=1 GO111MODULE=on go build $(OTHER_FLAGS) bulk_issuance

api_docs:
	@echo "Installing swagger-merger" && npm install swagger-merger -g
	@swagger-merger -i $(PWD)./interfaces/bulk_issuance_api.yaml -o $(PWD)/bundle.yaml

build: bulk_issuance-server
	@echo "building $(source)"
	@echo $(source)

run: ./bulk_issuance
	@echo "Running api server"
	./bulk_issuance --scheme http --port 5665

test:
	@go test -v ./...

docker:
	@make bulk_issuance-server
	@docker build -t $(IMAGE) .
publish:
	@docker push $(IMAGE)
