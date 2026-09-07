#!/bin/bash
# Creates a second database for the audit-service, alongside the default
# database used by core-api (POSTGRES_DB). Two databases, one Postgres
# instance: enough isolation for a local/portfolio setup without the
# operational overhead of two database containers.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE traintrack_audit;
EOSQL
