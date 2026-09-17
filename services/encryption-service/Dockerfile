FROM frolvlad/alpine-java:jdk8-slim
WORKDIR /app
ADD ./target/egov-enc-service-1.1.4.jar enc-service.jar

# [Auto-Remediation CWE-250] Enforce non-root execution
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
ENTRYPOINT ["java", "-jar", "enc-service.jar"]
