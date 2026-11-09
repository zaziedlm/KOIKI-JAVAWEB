#!/bin/sh
set -eu
# First initialization only. Never run against an existing volume.
for db in koiki_app koiki_bff koiki_keycloak; do
  db_password=$(cat "/run/secrets/$db")
  psql -v ON_ERROR_STOP=1 -U postgres -d postgres --set=db="$db" --set=password="$db_password" <<'SQL'
CREATE ROLE :"db" LOGIN PASSWORD :'password' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE DATABASE :"db" OWNER :"db";
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE :"db" TO :"db";
SQL
done
