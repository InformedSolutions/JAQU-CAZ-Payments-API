INSERT INTO payment(
	payment_id, payment_method, payment_provider_id, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
	VALUES
	  ('b71b72a5-902f-4a16-a91d-1a4463b801db', 'CREDIT_DEBIT_CARD', '12345test', 100, now(), now()),
	  ('b73a9b3c-d692-4e7e-b094-1715c5e4a036', 'CREDIT_DEBIT_CARD', '54321test', 200, now(), now()),
	  ('dabc1391-ff31-427a-8000-69037deb2d3a', 'CREDIT_DEBIT_CARD', '98765test', 100, now(), now());

INSERT INTO vehicle_entrant_payment(
	vehicle_entrant_payment_id, vehicle_entrant_id, payment_id, vrn, caz_id, travel_date, charge_paid, payment_status)
	VALUES
	  ('43ea77cc-93cb-4df3-b731-5244c0de9cc8', null, 'b71b72a5-902f-4a16-a91d-1a4463b801db', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-01', 50, 'PAID'),
	  ('688f8278-2f0f-4710-bb7c-6b0cca04c1bc', null, 'b71b72a5-902f-4a16-a91d-1a4463b801db', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-03', 50, 'PAID'),
	  ('9cc2dd1a-905e-4eaf-af85-0b14f95aab89', null, 'b73a9b3c-d692-4e7e-b094-1715c5e4a036', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-02', 100, 'PAID'),
	  ('00136ccf-e41b-4ce2-b044-7616aa589aa2', null, 'dabc1391-ff31-427a-8000-69037deb2d3a', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-02', 100, 'PAID'),
	  ('21b7049d-b978-482f-a882-4de6bb9d699c', null, 'b73a9b3c-d692-4e7e-b094-1715c5e4a036', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-04', 100, 'PAID');
