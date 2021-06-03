-- successful payment for 'ND84VSX' vrn
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('d80deb4e-0f8a-11ea-8dc9-93fa5be4476e', 'ext-payment-id-1', 'CREDIT_DEBIT_CARD', 'SUCCESS', 440, '2019-11-23T20:38:08.272Z', '2019-11-23T20:39:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('c59d0f46-0f8d-11ea-bbdd-9bfba959fef8', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-01', 'tariff-1', 88, 'PAID', 'USER'),
('c9801856-0f8d-11ea-bbdd-0fb9b9867da0', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-02', 'tariff-1', 88, 'PAID', 'USER'),
('ce083912-0f8d-11ea-bbdd-47debb103c06', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-03', 'tariff-1', 88, 'PAID', 'USER'),
('d22c4d6c-0f8d-11ea-bbdd-7ff4b1cc8ff1', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-04', 'tariff-1', 88, 'PAID', 'USER'),
('d572fea8-0f8d-11ea-bbdd-2b420f74f6f3', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-05', 'tariff-1', 88, 'PAID', 'USER'),
('3a9e60b5-1668-4465-83fa-6fe5f1b0d2a7', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-10', 'tariff-1', 88, 'FAILED', 'LA');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('7e2bf5c2-3cfc-11ea-b5aa-f7f8fb54cc82', 'c59d0f46-0f8d-11ea-bbdd-9bfba959fef8', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', true),
('82e32e00-3cfc-11ea-b5aa-5f9be6b00486', 'c9801856-0f8d-11ea-bbdd-0fb9b9867da0', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', true),
('86cf6a1a-3cfc-11ea-b5aa-bba49257036a', 'ce083912-0f8d-11ea-bbdd-47debb103c06', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', true),
('8b167ea6-3cfc-11ea-b5aa-ab2cc427f844', 'd22c4d6c-0f8d-11ea-bbdd-7ff4b1cc8ff1', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', true),
('8edf3ae6-3cfc-11ea-b5aa-57415abd2f79', 'd572fea8-0f8d-11ea-bbdd-2b420f74f6f3', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', true),
('e7363160-6fea-4cef-a44d-f00c24695c58', '3a9e60b5-1668-4465-83fa-6fe5f1b0d2a7', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', true);

-- failed payment for 'ND84VSX' vrn for '2019-11-06' and '2019-11-07' travel dates

insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp)
values ('2ed40346-0f90-11ea-bbdd-3f18854148eb', 'ext-payment-id-1', 'CREDIT_DEBIT_CARD', 'FAILED', 100, '2019-11-23T20:38:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
    clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('62320a6c-0f90-11ea-bbdd-b3fa7794610e', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-06', 'tariff-1', 50, 'NOT_PAID', 'USER'),
('65821f90-0f90-11ea-bbdd-9bba0c562c82', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-07', 'tariff-1', 50, 'NOT_PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('724bf7fa-3cfe-11ea-b5aa-531966956f40', '62320a6c-0f90-11ea-bbdd-b3fa7794610e', '2ed40346-0f90-11ea-bbdd-3f18854148eb', true),
('7907e2f2-3cfe-11ea-b5aa-bbeefb92190a', '65821f90-0f90-11ea-bbdd-9bba0c562c82', '2ed40346-0f90-11ea-bbdd-3f18854148eb', true);

-- charged back payment for 'ND84VSX' on '2019-11-08'

insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('de551408-1cca-11ea-8052-efd996bcf49c', 'ext-payment-id-4', 'CREDIT_DEBIT_CARD', 'SUCCESS', 540, '2019-11-23T20:38:08.272Z', '2019-11-23T20:39:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
    clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('5a4fdee8-1cd1-11ea-8bae-6f5a85323c9b', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-08', 'tariff-1', 180, 'CHARGEBACK', 'LA');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('14be708c-3d02-11ea-b5aa-e701cf5fde61', '62320a6c-0f90-11ea-bbdd-b3fa7794610e', 'de551408-1cca-11ea-8052-efd996bcf49c', false),
('1dfbca1e-3d02-11ea-b5aa-8bbf33d58b21', '65821f90-0f90-11ea-bbdd-9bba0c562c82', 'de551408-1cca-11ea-8052-efd996bcf49c', false),
('196bb0b8-3d02-11ea-b5aa-57749ddfa213', '5a4fdee8-1cd1-11ea-8bae-6f5a85323c9b', 'de551408-1cca-11ea-8052-efd996bcf49c', false);


-- successful payment for 'AB11CDE' vrn
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp, user_id)
values
('eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', 'ext-payment-id-3', 'CREDIT_DEBIT_CARD', 'SUCCESS', 140, '2019-11-23T20:38:08.272Z', '2019-11-23T20:39:08.272Z', 'b08d221f-5387-4b61-8732-9adcd3e9bb67');

insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp, user_id)
values
('d80deb4e-0f8a-11ea-8dc9-93fa5be4476f', 'ext-payment-id-3', 'CREDIT_DEBIT_CARD', 'SUCCESS', 140, null, '2019-11-23T20:39:08.272Z', 'b08d221f-5387-4b61-8732-9adcd3e9bb67');

insert into caz_payment.t_clean_air_zone_entrant_payment (clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('e218c724-102c-11ea-be9e-973e776167e1', 'AB11CDE', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-01', 'tariff-1', 70, 'PAID', 'USER'),
('e593130a-102c-11ea-be9e-975729b598b5', 'AB11CDE', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-02', 'tariff-1', 70, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('1e8ed2c2-3d03-11ea-b5aa-430f46682f15', 'e218c724-102c-11ea-be9e-973e776167e1', 'eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', true),
('250a771e-3d03-11ea-b5aa-f3262a385212', 'e593130a-102c-11ea-be9e-975729b598b5', 'eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', true);

-- payment for 'ND84VSX' in British Summer Time.
-- 23:39:09 GMT - day before the call
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('3c868536-2d7b-11eb-adc1-0242ac120002', 'ext-payment-id-10', 'CREDIT_DEBIT_CARD', 'SUCCESS', 440, '2020-09-01T23:38:08.272Z', '2020-09-01T23:39:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('3e7f648e-2d7b-11eb-adc1-0242ac120002', 'EST123', '53e03a28-0627-11ea-9511-ffaaee87e375', '2020-09-01', 'tariff-1', 88, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('646e75e8-2d82-11eb-adc1-0242ac120002', '3e7f648e-2d7b-11eb-adc1-0242ac120002', '3c868536-2d7b-11eb-adc1-0242ac120002', true);

-- 12:39:09 GMT - day of the call
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('793f1f60-2d7b-11eb-adc1-0242ac120002', 'ext-payment-id-10', 'CREDIT_DEBIT_CARD', 'SUCCESS', 440, '2020-09-02T12:38:09.272Z', '2020-09-02T12:39:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('d8e00128-2d7b-11eb-adc1-0242ac120002', 'EST123', '53e03a28-0627-11ea-9511-ffaaee87e375', '2020-09-02', 'tariff-1', 88, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('dfb849c8-2d7c-11eb-adc1-0242ac120002', 'd8e00128-2d7b-11eb-adc1-0242ac120002', '793f1f60-2d7b-11eb-adc1-0242ac120002', true);

-- 23:39:09 GMT - day of the call
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('63514686-2d7d-11eb-adc1-0242ac120002', 'ext-payment-id-10', 'CREDIT_DEBIT_CARD', 'SUCCESS', 440, '2020-09-02T23:38:09.272Z', '2020-09-02T12:39:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('819919d4-2d7d-11eb-adc1-0242ac120002', 'EST123', '53e03a28-0627-11ea-9511-ffaaee87e375', '2020-09-03', 'tariff-1', 88, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('89083fb0-2d7d-11eb-adc1-0242ac120002', '819919d4-2d7d-11eb-adc1-0242ac120002', '63514686-2d7d-11eb-adc1-0242ac120002', true);


-- payment for 'EST123' in British WINTER Time.
-- 23:39:09 GMT - day before the call
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('9018d7d8-2d7d-11eb-adc1-0242ac120002', 'ext-payment-id-10', 'CREDIT_DEBIT_CARD', 'SUCCESS', 440, '2020-11-01T23:38:08.272Z', '2020-11-01T23:39:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('2048b846-2d7e-11eb-adc1-0242ac120002', 'EST123', '53e03a28-0627-11ea-9511-ffaaee87e375', '2020-11-01', 'tariff-1', 88, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('2792d2a8-2d7e-11eb-adc1-0242ac120002', '2048b846-2d7e-11eb-adc1-0242ac120002', '9018d7d8-2d7d-11eb-adc1-0242ac120002', true);

-- 12:39:09 GMT - day of the call
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('2ed662dc-2d7e-11eb-adc1-0242ac120002', 'ext-payment-id-10', 'CREDIT_DEBIT_CARD', 'SUCCESS', 440, '2020-11-02T12:38:09.272Z', '2020-11-02T12:39:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('3456aa46-2d7e-11eb-adc1-0242ac120002', 'EST123', '53e03a28-0627-11ea-9511-ffaaee87e375', '2020-11-02', 'tariff-1', 88, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('3d3bfe72-2d7e-11eb-adc1-0242ac120002', '3456aa46-2d7e-11eb-adc1-0242ac120002', '2ed662dc-2d7e-11eb-adc1-0242ac120002', true);

-- 23:39:09 GMT - day of the call
insert into caz_payment.t_payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('4495f470-2d7e-11eb-adc1-0242ac120002', 'ext-payment-id-10', 'CREDIT_DEBIT_CARD', 'SUCCESS', 440, '2020-11-02T23:38:09.272Z', '2020-11-02T12:39:08.272Z');

insert into caz_payment.t_clean_air_zone_entrant_payment (
clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('4ce50314-2d7e-11eb-adc1-0242ac120002', 'EST123', '53e03a28-0627-11ea-9511-ffaaee87e375', '2020-11-03', 'tariff-1', 88, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('535433aa-2d7e-11eb-adc1-0242ac120002', '4ce50314-2d7e-11eb-adc1-0242ac120002', '4495f470-2d7e-11eb-adc1-0242ac120002', true);
