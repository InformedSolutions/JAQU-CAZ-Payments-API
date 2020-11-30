DELETE FROM caz_payment_audit.t_clean_air_zone_payment_detail CASCADE;
DELETE FROM caz_payment_audit.t_clean_air_zone_payment_master CASCADE;
DELETE FROM caz_payment.t_clean_air_zone_entrant_payment_match CASCADE;
DELETE FROM caz_payment.t_clean_air_zone_entrant_payment CASCADE;
DELETE FROM caz_payment.t_payment CASCADE;
DELETE FROM caz_payment_audit.logged_actions CASCADE;