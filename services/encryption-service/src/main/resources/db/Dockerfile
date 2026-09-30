FROM egovio/flyway:4.1.2

COPY ./migration/main /flyway/sql

COPY migrate.sh /usr/bin/migrate.sh

RUN chmod +x /usr/bin/migrate.sh

# [Auto-Remediation CWE-250] Enforce non-root execution
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
CMD ["/usr/bin/migrate.sh"]
