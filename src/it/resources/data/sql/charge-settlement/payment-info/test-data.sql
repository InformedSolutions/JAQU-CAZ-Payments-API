-- successful payment for 'ND84VSX' vrn
insert into payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('d80deb4e-0f8a-11ea-8dc9-93fa5be4476e', 'ext-payment-id-1', 'CREDIT_DEBIT_CARD', 'SUCCESS', 440, '2019-11-23T20:38:08.272Z', '2019-11-23T20:39:08.272Z');

insert into vehicle_entrant_payment (vehicle_entrant_payment_id, payment_id, vrn, caz_id, travel_date, charge_paid, payment_status)
values
('c59d0f46-0f8d-11ea-bbdd-9bfba959fef8', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-01', 88, 'PAID'),
('c9801856-0f8d-11ea-bbdd-0fb9b9867da0', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-02', 88, 'PAID'),
('ce083912-0f8d-11ea-bbdd-47debb103c06', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-03', 88, 'PAID'),
('d22c4d6c-0f8d-11ea-bbdd-7ff4b1cc8ff1', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-04', 88, 'PAID'),
('d572fea8-0f8d-11ea-bbdd-2b420f74f6f3', 'd80deb4e-0f8a-11ea-8dc9-93fa5be4476e', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-05', 88, 'PAID');

-- failed payment for 'ND84VSX' vrn for '2019-11-06' and '2019-11-07' travel dates
insert into payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp)
values ('2ed40346-0f90-11ea-bbdd-3f18854148eb', 'ext-payment-id-2', 'CREDIT_DEBIT_CARD', 'FAILED', 100, '2019-11-23T20:38:08.272Z');

insert into vehicle_entrant_payment (vehicle_entrant_payment_id, payment_id, vrn, caz_id, travel_date, charge_paid, payment_status)
values
('62320a6c-0f90-11ea-bbdd-b3fa7794610e', '2ed40346-0f90-11ea-bbdd-3f18854148eb', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-06', 50, 'NOT_PAID'),
('65821f90-0f90-11ea-bbdd-9bba0c562c82', '2ed40346-0f90-11ea-bbdd-3f18854148eb', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-07', 50, 'NOT_PAID');

-- successful payment with different statuses for 'ND84VSX' and '2019-11-06', '2019-11-07' and '2019-11-08' travel dates
insert into payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp)
values ('de551408-1cca-11ea-8052-efd996bcf49c', 'ext-payment-id-4', 'CREDIT_DEBIT_CARD', 'FAILED', 540, '2019-11-22T20:38:08.272Z');

insert into vehicle_entrant_payment (vehicle_entrant_payment_id, payment_id, vrn, caz_id, travel_date, charge_paid, payment_status)
values
('2cba820e-1ccb-11ea-8052-174f5b2d3113', 'de551408-1cca-11ea-8052-efd996bcf49c', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-06', 180, 'PAID'),
('2f4652c8-1ccb-11ea-8052-37ec9c9665b9', 'de551408-1cca-11ea-8052-efd996bcf49c', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-07', 180, 'REFUNDED'),
('5a4fdee8-1cd1-11ea-8bae-6f5a85323c9b', 'de551408-1cca-11ea-8052-efd996bcf49c', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-08', 180, 'CHARGEBACK');

-- successful payment for 'AB11CDE' vrn
insert into payment(payment_id, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp)
values ('eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', 'ext-payment-id-3', 'CREDIT_DEBIT_CARD', 'SUCCESS', 140, '2019-11-23T20:38:08.272Z', '2019-11-23T20:39:08.272Z');

insert into vehicle_entrant_payment (vehicle_entrant_payment_id, payment_id, vrn, caz_id, travel_date, charge_paid, payment_status)
values
('e218c724-102c-11ea-be9e-973e776167e1', 'eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', 'AB11CDE', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-01', 70, 'PAID'),
('e593130a-102c-11ea-be9e-975729b598b5', 'eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', 'AB11CDE', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-02', 70, 'PAID');