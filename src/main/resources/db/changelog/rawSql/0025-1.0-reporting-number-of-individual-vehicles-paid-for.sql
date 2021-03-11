CREATE OR REPLACE VIEW caz_reporting.number_of_individual_vehicles_paid_for AS
  SELECT payment_authorised_timestamp, 
  payment_provider_status, 
  count(*) as no_vehicles_paid_for 
  FROM 
    caz_payment.t_clean_air_zone_entrant_payment_match AS payment_match
    INNER JOIN 
    caz_payment.t_payment AS pay
    ON payment_match.payment_id = pay.payment_id
  WHERE payment_authorised_timestamp >= '2021-03-11'
  GROUP BY payment_authorised_timestamp, payment_provider_status