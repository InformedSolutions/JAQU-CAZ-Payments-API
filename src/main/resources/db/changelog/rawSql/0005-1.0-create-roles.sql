CREATE ROLE payments_readonly_role WITH
NOLOGIN
NOSUPERUSER
NOINHERIT
NOCREATEDB
NOCREATEROLE
NOREPLICATION;

GRANT CONNECT ON DATABASE vehicle_compliance TO payments_readonly_role;
GRANT USAGE ON SCHEMA caz_payment TO payments_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_payment TO payments_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_payment GRANT SELECT ON TABLES TO payments_readonly_role;
GRANT USAGE ON SCHEMA caz_payment_audit TO payments_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_payment_audit TO payments_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_payment_audit GRANT SELECT ON TABLES TO payments_readonly_role;

CREATE ROLE payments_readwrite_role WITH
NOLOGIN
NOSUPERUSER
NOINHERIT
NOCREATEDB
NOCREATEROLE
NOREPLICATION;

GRANT CONNECT ON DATABASE vehicle_compliance TO payments_readwrite_role;
GRANT USAGE ON SCHEMA caz_payment TO payments_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA caz_payment TO payments_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_payment GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO payments_readwrite_role;
GRANT USAGE ON SCHEMA caz_payment_audit TO payments_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA caz_payment_audit TO payments_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_payment_audit GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO payments_readwrite_role;