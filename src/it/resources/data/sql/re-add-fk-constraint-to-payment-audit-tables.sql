TRUNCATE caz_payment_audit.t_clean_air_zone_payment_master CASCADE;
TRUNCATE caz_payment_audit.logged_actions;

ALTER TABLE caz_payment_audit.t_clean_air_zone_payment_detail
ADD CONSTRAINT t_clean_air_zone_payment_detail_payment_id_fkey FOREIGN KEY (payment_id)
        REFERENCES caz_payment.t_payment (payment_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;