TRUNCATE caz_payment_audit.t_clean_air_zone_payment_master CASCADE;
TRUNCATE caz_payment_audit.logged_actions;

ALTER TABLE caz_payment_audit.t_clean_air_zone_payment_detail
DROP CONSTRAINT IF EXISTS t_clean_air_zone_payment_detail_payment_id_fkey;