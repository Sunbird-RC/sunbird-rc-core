FROM golang:1.20-alpine
RUN  apk add make git gcc musl-dev
WORKDIR /app/metrics
COPY . .
RUN go mod download
RUN make deps
RUN GOFLAGS=" -tags=musl" SPEC_FILE="./interfaces/metrics-api.yaml" make all
EXPOSE 8070
CMD [ "/app/metrics/metrics", "--scheme", "http", "--port", "8070", "--host", "0.0.0.0" ]