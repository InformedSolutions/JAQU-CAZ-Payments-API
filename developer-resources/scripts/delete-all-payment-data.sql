-- This script is to be executed against the Vehicle Compliance database and clears all payment data

-- Note that due to the size of tables, it is recommended to run each line of this script in turn.
DELETE FROM caz_payment_audit.t_clean_air_zone_payment_detail CASCADE;
DELETE FROM caz_payment_audit.t_clean_air_zone_payment_master CASCADE;
DELETE FROM caz_payment.t_clean_air_zone_entrant_payment_match CASCADE;
DELETE FROM caz_payment.t_clean_air_zone_entrant_payment CASCADE;
TRUNCATE TABLE caz_payment.t_payment RESTART IDENTITY CASCADE;
ALTER SEQUENCE caz_payment.reference_number RESTART WITH 1627;
DELETE FROM caz_payment_audit.logged_actions CASCADE;

-- Verify no records are now present
SELECT COUNT(1) FROM caz_payment_audit.t_clean_air_zone_payment_detail;
SELECT COUNT(1) FROM caz_payment_audit.t_clean_air_zone_payment_master;
SELECT COUNT(1) FROM caz_payment.t_clean_air_zone_entrant_payment_match;
SELECT COUNT(1) FROM caz_payment.t_clean_air_zone_entrant_payment;
SELECT COUNT(1) FROM caz_payment.t_payment;
SELECT COUNT(1) FROM caz_payment_audit.logged_actions;