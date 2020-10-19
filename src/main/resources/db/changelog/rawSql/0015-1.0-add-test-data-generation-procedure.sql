/*
 * Creates a stored procedure to inject synthetic test data into the payment schema. This is intended for use
 * to ensure scaling of application performance when table row count increase. The number of test records to 
 * be generated can be supplied via a parameter when invoking the procedure.
 */

CREATE OR REPLACE PROCEDURE caz_payment.insert_test_payment_records(number_of_test_records integer)
AS $$
DECLARE
  _entrant_payment_id uuid := uuid_generate_v4();
  _payment_id uuid := uuid_generate_v4();
  _entrant_payment_match_id uuid := uuid_generate_v4();
BEGIN
   FOR i IN 1..number_of_test_records LOOP
	 
   	/**
   	 * Note that the identifiers are bound to variables in this portion of the procedure to permit a binding between tables
   	 * for referential integritry constraints.
   	 */
	 _entrant_payment_id := uuid_generate_v4();
	 _payment_id := uuid_generate_v4();
	 _entrant_payment_match_id := uuid_generate_v4();
	 
	 INSERT INTO caz_payment.t_payment(payment_id, payment_method, payment_provider_id, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp, insert_timestamp, telephone_payment)
     VALUES (_payment_id, 'CREDIT_DEBIT_CARD', md5(random()::text), 'SUCCESS', 500, now()::timestamp, now()::timestamp, now()::timestamp, false);
	
     INSERT INTO caz_payment.t_clean_air_zone_entrant_payment(clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, vehicle_entrant_captured, update_actor, insert_timestamp)
	 VALUES (_entrant_payment_id, UPPER(substr(md5(random()::text), 0, 7)), '5cd7441d-766f-48ff-b8ad-1809586fea37', now()::date, 'BCC01-HEAVY GOODS VEHICLE', 500, 'PAID', true, 'USER', now()::timestamp);
	
	 INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
	 VALUES (_entrant_payment_match_id, _entrant_payment_id, _payment_id, true);
	  
   END LOOP;
   COMMIT;
END;$$
LANGUAGE PLPGSQL;