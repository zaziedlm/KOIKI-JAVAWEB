#!/bin/sh
set -eu
# Read secrets without shell tracing; password never appears in the SQL script source.
app_password=$(cat /run/secrets/app_db_password)
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=app_password="$app_password" <<'SQL'
CREATE ROLE koiki LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
ALTER DATABASE koiki OWNER TO koiki;
GRANT USAGE, CREATE ON SCHEMA public TO koiki;
SQL
