-- V2: durable one-attempt marker; retained after process/DB loss means reauthentication.
ALTER TABLE bff_session ADD COLUMN refresh_pending boolean NOT NULL DEFAULT false;
