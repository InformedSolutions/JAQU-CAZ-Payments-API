INSERT INTO caz_payment.t_payment(
	payment_id, payment_method, payment_provider_status, payment_provider_id, total_paid, payment_submitted_timestamp, payment_authorised_timestamp, central_reference_number)
	VALUES
	  ('b71b72a5-902f-4a16-a91d-1a4463b801db', 'CREDIT_DEBIT_CARD', 'SUCCESS', '12345test', 100, now(), now(), 3000),
	  ('b73a9b3c-d692-4e7e-b094-1715c5e4a036', 'CREDIT_DEBIT_CARD', 'SUCCESS', '54321test', 200, now(), now(), 3001);

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment(
	clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, charge, tariff_code, payment_status, case_reference, update_actor)
	VALUES
	  -- Valid with PAID status
	  ('43ea77cc-93cb-4df3-b731-5244c0de9cc8', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-01', 50, 'tariffCode!', 'PAID', 'case-reference123', 'USER'),

    -- Valid with REFUNDED status
	  ('688f8278-2f0f-4710-bb7c-6b0cca04c1bc', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-02', 50, 'tariffCode!', 'REFUNDED', 'case-reference123', 'LA'),

    -- Valid with NOT_PAID status
	  ('9e8a8d54-25ff-43e6-85f6-01a4c8f2a2d2', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-03', 50, 'tariffCode!', 'NOT_PAID', 'case-reference123', 'VCCS_API'),

    -- Multiple payment only latest PAID
    ('9c3f36fa-0ddd-4204-b106-3fc90af6efb6', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-04', 50, 'tariffCode!', 'PAID', 'case-reference123', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(
	id, clean_air_zone_entrant_payment_id, payment_id, latest)
	VALUES
  ('a277431b-4b42-4f61-af43-b262a0467807', '43ea77cc-93cb-4df3-b731-5244c0de9cc8', 'b73a9b3c-d692-4e7e-b094-1715c5e4a036', true),
  ('2d34ec3b-6ca3-4a05-9301-8462b46e1cc0', '688f8278-2f0f-4710-bb7c-6b0cca04c1bc', 'b73a9b3c-d692-4e7e-b094-1715c5e4a036', true),
  ('b5d85dbe-3ece-42d5-be81-d57e41471c5f', '9e8a8d54-25ff-43e6-85f6-01a4c8f2a2d2', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true),
  ('27e276bc-aed1-4b4b-904f-5d94548d8dfe', '9c3f36fa-0ddd-4204-b106-3fc90af6efb6', 'b73a9b3c-d692-4e7e-b094-1715c5e4a036', true),
  ('512b0982-3e7e-4be6-b0ef-2b3310627a0a', '9c3f36fa-0ddd-4204-b106-3fc90af6efb6', 'b71b72a5-902f-4a16-a91d-1a4463b801db', false);