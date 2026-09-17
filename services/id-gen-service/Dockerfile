FROM frolvlad/alpine-java:jdk8-slim
WORKDIR /app
ADD ./target/egov-idgen-1.2.4-SNAPSHOT.jar id-gen-service.jar

# [Auto-Remediation CWE-250] Enforce non-root execution
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
ENTRYPOINT ["java", "-jar", "id-gen-service.jar"]
