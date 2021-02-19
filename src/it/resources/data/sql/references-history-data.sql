insert into caz_payment.t_payment(payment_id, central_reference_number, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp, user_id, operator_id)
values
('eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', 2000, 'ext-payment-id-3', 'CREDIT_DEBIT_CARD', 'SUCCESS', 140, '2020-05-01 11:43:41.569', '2019-11-23T20:39:08.272Z', 'b08d221f-5387-4b61-8732-9adcd3e9bb67', 'e9a92b87-057a-4578-afb1-61d8b9af1569');

insert into caz_payment.t_payment(payment_id, central_reference_number, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp, user_id, operator_id)
values
('d80deb4e-0f8a-11ea-8dc9-93fa5be4476f', 2001, 'ext-payment-id-3', 'CREDIT_DEBIT_CARD', 'SUCCESS', 140, '2020-05-01 11:43:41.569', '2019-11-23T20:39:08.272Z', 'b08d221f-5387-4b61-8732-9adcd3e9bb67', 'e9a92b87-057a-4578-afb1-61d8b9af1569');

insert into caz_payment.t_clean_air_zone_entrant_payment (clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('e218c724-102c-11ea-be9e-973e776167e1', 'AB11CDE', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-01', 'tariff-1', 70, 'PAID', 'USER'),
('e593130a-102c-11ea-be9e-975729b598b5', 'AB11CDE', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-02', 'tariff-1', 70, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('1e8ed2c2-3d03-11ea-b5aa-430f46682f15', 'e218c724-102c-11ea-be9e-973e776167e1', 'eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', true),
('250a771e-3d03-11ea-b5aa-f3262a385212', 'e593130a-102c-11ea-be9e-975729b598b5', 'eb3f1a6a-102c-11ea-be9e-2b1c2964eba8', true);

-- not unique clean air zones in entrant payment table for the same payment
insert into caz_payment.t_payment(payment_id, central_reference_number, payment_provider_id, payment_method, payment_provider_status, total_paid, payment_submitted_timestamp, payment_authorised_timestamp, user_id, operator_id)
values
('eb3f1a6a-102c-11ea-be9e-2b1c2964eba6', 2500, 'ext-payment-id-3', 'CREDIT_DEBIT_CARD', 'SUCCESS', 140, '2020-05-01 11:43:41.569', '2019-11-23T20:39:08.272Z', 'b08d221f-5387-4b61-8732-9adcd3e9bb67', 'e9a92b87-057a-4578-afb1-61d8b9af1569');

insert into caz_payment.t_clean_air_zone_entrant_payment (clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
('e218c724-102c-11ea-be9e-973e776167e2', 'AB11CDE', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-03', 'tariff-1', 70, 'PAID', 'USER'),
('e593130a-102c-11ea-be9e-975729b598b6', 'AB11CDE', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-04', 'tariff-1', 70, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(id, clean_air_zone_entrant_payment_id, payment_id, latest)
VALUES
('1e8ed2c2-3d03-11ea-b5aa-430f46682f16', 'e218c724-102c-11ea-be9e-973e776167e2', 'eb3f1a6a-102c-11ea-be9e-2b1c2964eba6', true),
('250a771e-3d03-11ea-b5aa-f3262a385213', 'e593130a-102c-11ea-be9e-975729b598b6', 'eb3f1a6a-102c-11ea-be9e-2b1c2964eba6', true);