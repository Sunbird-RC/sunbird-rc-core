FROM golang:1.23-alpine AS notification-service

RUN apk add make git gcc musl-dev

WORKDIR /app/notification-service

COPY . .
RUN go mod download
RUN make deps
RUN GOFLAGS=" -tags=musl" SPEC_FILE="./notification-service.yaml" make all

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8765
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8765/health || exit 1
CMD ["/app/notification-service/notification-service", "--scheme", "http", "--port", "8765", "--host", "0.0.0.0"]