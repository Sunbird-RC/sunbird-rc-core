FROM golang:1.24-alpine
RUN apk add make git
WORKDIR /app/metrics
COPY . .
RUN go mod download
RUN CGO_ENABLED=0 SPEC_FILE="./interfaces/metrics-api.yaml" make all
EXPOSE 8070
CMD [ "/app/metrics/metrics", "--scheme", "http", "--port", "8070", "--host", "0.0.0.0" ]
