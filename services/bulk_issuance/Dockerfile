FROM golang:1.20-alpine
RUN  apk add make git gcc musl-dev
WORKDIR /app/bulk_issuance
COPY . .
RUN go mod download
RUN make deps
RUN go test -v ./...
RUN GOFLAGS=" -tags=musl" SPEC_FILE="./interfaces/bulk_issuance-api.yaml" make all
EXPOSE 5665
CMD [ "/app/bulk_issuance/bulk_issuance", "--scheme", "http", "--port", "5665", "--host", "0.0.0.0" ]