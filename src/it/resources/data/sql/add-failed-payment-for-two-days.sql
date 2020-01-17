INSERT INTO caz_payment.t_payment(
	payment_id, payment_method, payment_provider_id, total_paid, payment_provider_status, payment_submitted_timestamp)
	VALUES
	  ('b71b72a5-902f-4a16-a91d-1a4463b801db', 'CREDIT_DEBIT_CARD', '12345test', 4200, 'FAILED', now());

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment(
	clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
	VALUES
	  ('9cc2dd1a-905e-4eaf-af85-0b14f95aab89', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-02', 'tariff-one', 2100, 'NOT_PAID', 'USER'),
	  ('303221c8-3871-11ea-9691-7387f945f3a5', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-10', 'tariff-two', 2100, 'NOT_PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
  ('74af34d6-255d-495d-bbce-aa5bcea9736d', '9cc2dd1a-905e-4eaf-af85-0b14f95aab89', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true),
  ('51fb004a-3871-11ea-9691-d39b9a111a08', '303221c8-3871-11ea-9691-7387f945f3a5', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true);