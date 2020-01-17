INSERT INTO caz_payment.t_payment(
	payment_id, payment_method, payment_provider_id, total_paid, payment_provider_status, payment_submitted_timestamp)
	VALUES
	  ('db720d46-3902-11ea-a0e6-9395f39911bf', 'CREDIT_DEBIT_CARD', '12345test', 6300, 'FAILED', now());

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment(
	clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
	VALUES
	  ('badb46ce-3902-11ea-a0e6-6bf06258ede5', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-10', 'tariff-one', 2100, 'NOT_PAID', 'USER'),
	  ('cf54fd70-3902-11ea-a0e6-fbf575fb00ee', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-11', 'tariff-two', 2100, 'NOT_PAID', 'USER'),
	  ('f91142fe-3902-11ea-a0e6-1b6922fa68ee', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-12', 'tariff-two', 2100, 'NOT_PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
  ('74af34d6-255d-495d-bbce-aa5bcea9736d', 'badb46ce-3902-11ea-a0e6-6bf06258ede5', 'db720d46-3902-11ea-a0e6-9395f39911bf', true),
  ('51fb004a-3871-11ea-9691-d39b9a111a08', 'cf54fd70-3902-11ea-a0e6-fbf575fb00ee', 'db720d46-3902-11ea-a0e6-9395f39911bf', true),
  ('00b37752-3903-11ea-a0e6-03458a219d45', 'f91142fe-3902-11ea-a0e6-1b6922fa68ee', 'db720d46-3902-11ea-a0e6-9395f39911bf', true);