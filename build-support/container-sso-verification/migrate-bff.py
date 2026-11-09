"""One transaction, advisory migration lock, checksum rejection before any DDL."""
import argparse
import hashlib
from pathlib import Path
from preflight import load, sql


def migrate(work, compose, content=None):
    ddl = (work / 'recipe/bff-migration.sql').read_text() if content is None else content
    checksum = hashlib.sha256(ddl.encode()).hexdigest()
    query = r"""BEGIN;
SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='30s';
SELECT pg_advisory_xact_lock(731091011);
CREATE TABLE IF NOT EXISTS bff_schema_history (
 version integer PRIMARY KEY CHECK (version > 0),
 checksum char(64) NOT NULL CHECK (checksum ~ '^[a-f0-9]{64}$'),
 applied_at timestamptz NOT NULL DEFAULT current_timestamp
);
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM bff_schema_history WHERE version=1 AND checksum <> '__CHECKSUM__') THEN
   RAISE EXCEPTION 'BFF migration checksum mismatch';
 END IF;
 IF EXISTS (SELECT 1 FROM bff_schema_history WHERE version<>1) THEN
   RAISE EXCEPTION 'Unexpected BFF schema version';
 END IF;
END $$;
SELECT NOT EXISTS(SELECT 1 FROM bff_schema_history WHERE version=1) AS apply \gset
\if :apply
__DDL__
INSERT INTO bff_schema_history(version,checksum) VALUES (1,'__CHECKSUM__');
\endif
COMMIT;
SELECT version||':'||checksum FROM bff_schema_history ORDER BY version;
""".replace('__CHECKSUM__', checksum).replace('__DDL__', ddl)
    return sql(work, compose, 'koiki_bff', 'koiki_bff', query)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--work-dir', type=Path, required=True)
    a = p.parse_args()
    work, record, compose = load(a.work_dir)
    print(migrate(work, compose).stdout.strip())
