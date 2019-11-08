insert into PAYMENT (payment_id, payment_method, payment_provider_id,
                     payment_provider_status, total_paid)
values
('1883736c-016f-11ea-999f-974122a6ca41', 'CREDIT_DEBIT_CARD', null, null, 100);

insert into vehicle_entrant_payment(payment_id, travel_date, caz_id, vrn, payment_status, charge_paid)
values
('1883736c-016f-11ea-999f-974122a6ca41', now(), 'b8e53786-c5ca-426a-a701-b14ee74857d4', 'ND84VSX',
 'NOT_PAID', 100);