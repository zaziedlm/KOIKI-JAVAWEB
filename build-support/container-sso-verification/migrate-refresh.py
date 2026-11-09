"""S5-only V2, preserving the accepted V1 checksum and migration history."""
import hashlib
from preflight import sql

def migrate(work,compose):
    base=hashlib.sha256((work/'recipe/bff-migration.sql').read_bytes()).hexdigest()
    ddl=(work/'recipe/bff-refresh-migration.sql').read_text();checksum=hashlib.sha256(ddl.encode()).hexdigest()
    query=r"""BEGIN;
SET LOCAL lock_timeout='5s';
SELECT pg_advisory_xact_lock(731091011);
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM bff_schema_history WHERE version=1 AND checksum='__BASE__') OR EXISTS(SELECT 1 FROM bff_schema_history WHERE version NOT IN (1,2)) THEN RAISE EXCEPTION 'Unexpected BFF base'; END IF;
 IF EXISTS(SELECT 1 FROM bff_schema_history WHERE version=2 AND checksum<>'__CHECKSUM__') THEN RAISE EXCEPTION 'BFF V2 checksum mismatch'; END IF;
END $$;
SELECT NOT EXISTS(SELECT 1 FROM bff_schema_history WHERE version=2) AS apply \gset
\if :apply
__DDL__
INSERT INTO bff_schema_history(version,checksum) VALUES(2,'__CHECKSUM__');
\endif
COMMIT;
SELECT version||':'||checksum FROM bff_schema_history ORDER BY version;
""".replace('__BASE__',base).replace('__CHECKSUM__',checksum).replace('__DDL__',ddl)
    return sql(work,compose,'koiki_bff','koiki_bff',query)
