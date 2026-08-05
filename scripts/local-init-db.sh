#!/bin/bash
# Creates the databases the Node services connect to, at first Postgres start.
#
# POSTGRES_DB only creates one database (registry). identity-service,
# credential-schema and credentials-service each point at their own, and Prisma
# will not create a missing database — it fails at boot with "database ... does
# not exist". The cloud deployment happens to have had these created by hand,
# which is why this only shows up locally.
#
# Runs from /docker-entrypoint-initdb.d, so it executes ONCE on an empty data
# directory. `docker compose down -v` to re-run it.
set -e

for db in identity credential credential_schema; do
  echo "  creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-SQL
    SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (
      SELECT FROM pg_database WHERE datname = '$db'
    )\gexec
SQL
done

echo "  databases ready: registry (via POSTGRES_DB), identity, credential, credential_schema"
