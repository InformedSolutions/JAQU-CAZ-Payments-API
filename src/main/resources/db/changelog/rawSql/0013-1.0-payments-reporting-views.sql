CREATE OR REPLACE VIEW caz_reporting.number_payments_by_hour_caz_status_caz_method AS
  SELECT DATE_TRUNC('hour', pay.insert_timestamp) AS hour, 
    caz_name, 
    payment_provider_status, 
    payment_method, 
    COUNT(*) AS no_payments 
  FROM caz_payment.t_payment pay
    INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match pay_match
      ON pay.payment_id = pay_match.payment_id 
    INNER JOIN caz_payment.t_clean_air_zone_entrant_payment entrant_pay
      ON pay_match.clean_air_zone_entrant_payment_id = entrant_pay.clean_air_zone_entrant_payment_id
    INNER JOIN caz_reporting.t_clean_air_zone caz
      ON caz.clean_air_zone_id = entrant_pay.clean_air_zone_id
  GROUP BY hour, caz_name, payment_provider_status, payment_method;