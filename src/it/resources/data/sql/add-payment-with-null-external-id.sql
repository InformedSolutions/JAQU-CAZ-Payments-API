insert into caz_payment.t_payment (payment_id, payment_method, payment_provider_id,
                     payment_provider_status, total_paid)
values
('1883736c-016f-11ea-999f-974122a6ca41', 'CREDIT_DEBIT_CARD', null, null, 100);

insert into caz_payment.t_clean_air_zone_entrant_payment(travel_date, clean_air_zone_id, vrn, payment_status, charge, tariff_code)
values
(now(), 'b8e53786-c5ca-426a-a701-b14ee74857d4', 'ND84VSX', 'NOT_PAID', 100, "TariffCode");