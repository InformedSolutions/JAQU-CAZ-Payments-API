CREATE OR REPLACE VIEW caz_reporting.successful_payments_for_account_vehicles AS
SELECT TO_DATE(ent_pay.insert_timestamp::text, 'YYYY/MM/DD') AS day,
    caz.caz_name,
    COUNT(DISTINCT(pay.payment_provider_status, pay.payment_id))
   FROM caz_payment.t_clean_air_zone_entrant_payment ent_pay
     JOIN caz_payment.t_clean_air_zone_entrant_payment_match pay_match
	 ON ent_pay.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
     JOIN caz_payment.t_payment pay ON pay_match.payment_id = pay.payment_id
     JOIN caz_reporting.t_clean_air_zone caz ON ent_pay.clean_air_zone_id = caz.clean_air_zone_id
  WHERE pay.user_id IS NOT NULL AND pay.payment_provider_status= 'SUCCESS'
  GROUP BY (TO_DATE(ent_pay.insert_timestamp::text, 'YYYY/MM/DD')), caz.caz_name
  ORDER BY (TO_DATE(ent_pay.insert_timestamp::text, 'YYYY/MM/DD'));

CREATE OR REPLACE VIEW caz_reporting.unsuccessful_payments_for_account_vehicles AS
SELECT TO_DATE(ent_pay.insert_timestamp::text, 'YYYY/MM/DD') AS day,
    caz.caz_name,
    COUNT(DISTINCT(pay.payment_provider_status, pay.payment_id))
   FROM caz_payment.t_clean_air_zone_entrant_payment ent_pay
     JOIN caz_payment.t_clean_air_zone_entrant_payment_match pay_match
	 ON ent_pay.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
     JOIN caz_payment.t_payment pay ON pay_match.payment_id = pay.payment_id
     JOIN caz_reporting.t_clean_air_zone caz ON ent_pay.clean_air_zone_id = caz.clean_air_zone_id
  WHERE pay.user_id IS NOT NULL AND pay.payment_provider_status= 'FAILED'
  GROUP BY (TO_DATE(ent_pay.insert_timestamp::text, 'YYYY/MM/DD')), caz.caz_name
  ORDER BY (TO_DATE(ent_pay.insert_timestamp::text, 'YYYY/MM/DD'));

CREATE OR REPLACE VIEW caz_reporting.successful_payments_profile_for_account_vehicles_by_payment_type AS
SELECT date_trunc('day'::text, pay.payment_authorised_timestamp) AS day,
    caz.caz_name,
	pay.payment_method,
    COUNT(DISTINCT(pay.payment_provider_status, pay.payment_id))
   FROM caz_payment.t_payment pay
     JOIN caz_payment.t_clean_air_zone_entrant_payment_match pay_match ON pay.payment_id = pay_match.payment_id
     JOIN caz_payment.t_clean_air_zone_entrant_payment pay_entrant ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
     JOIN caz_reporting.t_clean_air_zone caz ON pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE pay.user_id IS NOT NULL AND pay.payment_provider_status= 'SUCCESS'
  GROUP BY (date_trunc('day'::text, pay.payment_authorised_timestamp)), caz.caz_name, pay.payment_method
  ORDER BY (date_trunc('day'::text, pay.payment_authorised_timestamp));


CREATE OR REPLACE VIEW caz_reporting.general_payments_profile_for_all_vehicles AS
 SELECT date_trunc('day'::text, pay.payment_authorised_timestamp) AS day,
    caz.caz_name,
	pay.payment_method,
	pay.payment_provider_status,
    COUNT(DISTINCT(pay.payment_provider_status, pay.payment_id))
   FROM caz_payment.t_payment pay
     JOIN caz_payment.t_clean_air_zone_entrant_payment_match pay_match ON pay.payment_id = pay_match.payment_id
     JOIN caz_payment.t_clean_air_zone_entrant_payment pay_entrant ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
     JOIN caz_reporting.t_clean_air_zone caz ON pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  GROUP BY (date_trunc('day'::text, pay.payment_authorised_timestamp)), caz.caz_name, pay.payment_method, pay.payment_provider_status
  ORDER BY (date_trunc('day'::text, pay.payment_authorised_timestamp));