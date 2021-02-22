-- operator_id: 24f630ec-47c6-4cd0-b8aa-1e05a1463492
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, operator_id, payment_submitted_timestamp, payment_authorised_timestamp, insert_timestamp, central_reference_number)
values ('391017e8-e2d5-467f-b271-f6cf966eb931', 'ext-payment-id-1', 'CREDIT_DEBIT_CARD', 'SUCCESS', 352, '24f630ec-47c6-4cd0-b8aa-1e05a1463492', '2019-11-23T20:38:08.272Z', '2019-11-23T20:39:08.272Z', '2019-11-23T20:39:08.272Z', 87);

insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, operator_id, payment_submitted_timestamp, payment_authorised_timestamp, insert_timestamp, central_reference_number)
values ('485dc5d0-14e1-4007-997e-c2d3cf8b6d1e', 'ext-payment-id-2', 'CREDIT_DEBIT_CARD', 'SUCCESS', 260, '24f630ec-47c6-4cd0-b8aa-1e05a1463492', '2019-11-23T20:38:08.272Z', '2019-11-23T20:39:08.272Z', '2019-11-24T20:39:08.272Z', 998);

insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, operator_id, payment_submitted_timestamp, payment_authorised_timestamp, insert_timestamp, central_reference_number)
values ('282ccd65-1319-4b3b-a21c-dfe58809bedf', 'ext-payment-id-3', 'CREDIT_DEBIT_CARD', 'SUCCESS', 280, '24f630ec-47c6-4cd0-b8aa-1e05a1463492', '2019-11-23T20:38:08.272Z', '2019-11-23T20:39:08.272Z', '2019-11-25T20:39:08.272Z', 1881);

insert into caz_payment.t_clean_air_zone_entrant_payment (
    clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('c59d0f46-0f8d-11ea-bbdd-9bfba959fef8', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-01', 'tariff-1', 88, 'PAID', 'USER'),
('c9801856-0f8d-11ea-bbdd-0fb9b9867da0', 'MD84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-02', 'tariff-1', 88, 'PAID', 'USER'),
('ce083912-0f8d-11ea-bbdd-47debb103c06', 'OD84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-03', 'tariff-1', 88, 'PAID', 'USER'),
('d22c4d6c-0f8d-11ea-bbdd-7ff4b1cc8ff1', 'PD84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-04', 'tariff-1', 88, 'PAID', 'USER'),
('d572fea8-0f8d-11ea-bbdd-2b420f74f6f3', 'QD84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-05', 'tariff-1', 260, 'PAID', 'USER'),
('057e7b23-10ac-4ed2-b21b-cf53abd653bd', 'RD84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-06', 'tariff-1', 280, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('7e2bf5c2-3cfc-11ea-b5aa-f7f8fb54cc82', 'c59d0f46-0f8d-11ea-bbdd-9bfba959fef8', '391017e8-e2d5-467f-b271-f6cf966eb931', true),
('82e32e00-3cfc-11ea-b5aa-5f9be6b00486', 'c9801856-0f8d-11ea-bbdd-0fb9b9867da0', '391017e8-e2d5-467f-b271-f6cf966eb931', true),
('86cf6a1a-3cfc-11ea-b5aa-bba49257036a', 'ce083912-0f8d-11ea-bbdd-47debb103c06', '391017e8-e2d5-467f-b271-f6cf966eb931', true),
('8b167ea6-3cfc-11ea-b5aa-ab2cc427f844', 'd22c4d6c-0f8d-11ea-bbdd-7ff4b1cc8ff1', '391017e8-e2d5-467f-b271-f6cf966eb931', false),
-- for payment '485dc5d0-14e1-4007-997e-c2d3cf8b6d1e'
('c6e5f439-998f-4fc6-952d-b02090afc293', 'd572fea8-0f8d-11ea-bbdd-2b420f74f6f3', '485dc5d0-14e1-4007-997e-c2d3cf8b6d1e', true),
('2980b131-8679-4f45-b466-1d704219388c', 'd22c4d6c-0f8d-11ea-bbdd-7ff4b1cc8ff1', '485dc5d0-14e1-4007-997e-c2d3cf8b6d1e', true),
-- for payment '282ccd65-1319-4b3b-a21c-dfe58809bedf'
('62127799-74b5-4587-9eb2-2d3e89468173', '057e7b23-10ac-4ed2-b21b-cf53abd653bd', '282ccd65-1319-4b3b-a21c-dfe58809bedf', true);

UPDATE caz_payment.t_clean_air_zone_entrant_payment
SET payment_status = 'CHARGEBACK', update_actor = 'LA'
WHERE clean_air_zone_entrant_payment_id = 'c59d0f46-0f8d-11ea-bbdd-9bfba959fef8';

UPDATE caz_payment.t_clean_air_zone_entrant_payment
SET payment_status = 'REFUNDED', update_actor = 'LA'
WHERE clean_air_zone_entrant_payment_id = 'c59d0f46-0f8d-11ea-bbdd-9bfba959fef8';

UPDATE caz_payment.t_clean_air_zone_entrant_payment
SET payment_status = 'CHARGEBACK', update_actor = 'LA'
WHERE clean_air_zone_entrant_payment_id = 'c9801856-0f8d-11ea-bbdd-0fb9b9867da0';

UPDATE caz_payment.t_clean_air_zone_entrant_payment
SET tariff_code = 'tariff-2', update_actor = 'USER'
WHERE clean_air_zone_entrant_payment_id = 'ce083912-0f8d-11ea-bbdd-47debb103c06';

UPDATE caz_payment.t_clean_air_zone_entrant_payment
SET payment_status = 'CHARGEBACK', update_actor = 'LA'
WHERE clean_air_zone_entrant_payment_id = 'd22c4d6c-0f8d-11ea-bbdd-7ff4b1cc8ff1';
