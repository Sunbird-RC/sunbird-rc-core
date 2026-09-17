FROM golang:1.25-alpine
RUN apk add make git
WORKDIR /app/metrics
COPY . .
RUN go mod download
RUN CGO_ENABLED=0 SPEC_FILE="./interfaces/metrics-api.yaml" make all
EXPOSE 8070

# [Auto-Remediation CWE-250] Enforce non-root execution
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
CMD [ "/app/metrics/metrics", "--scheme", "http", "--port", "8070", "--host", "0.0.0.0" ]
