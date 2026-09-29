-- PostgreSQL should roll this failed migration back atomically.
CREATE TABLE probe_failed_upgrade (id INT PRIMARY KEY);
SELECT 1 / 0;
