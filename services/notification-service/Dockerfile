FROM golang:1.15-alpine AS notification-service

RUN  apk add make git gcc musl-dev
# Set the Current Working Directory inside the container

WORKDIR /app/notification-service

COPY . .
RUN go mod download
RUN make deps
RUN GOFLAGS=" -tags=musl" SPEC_FILE="./notification-service.yaml" make all
EXPOSE 8765
CMD ["/app/notification-service/notification-service", "--scheme", "http", "--port", "8765", "--host", "0.0.0.0"]