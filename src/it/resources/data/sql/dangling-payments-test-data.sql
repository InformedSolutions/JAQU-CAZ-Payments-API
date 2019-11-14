--- This script inserts some dangling payments alongside with candidates


-- NOT dangling one since it was submitted less than 90 minutes ago
insert into PAYMENT (payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'aac1ksqi26f9t2h7q3henmlamc',
 'CREATED',
 100,
 now() - interval '88 minutes',
 null
 );

-- NOT dangling one since it has NULL payment_provider_id (a payment done in LA)
insert into PAYMENT (payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 null,
 null,
 100,
 null,
 null
);

-------------- PAYMENTS in 'final' states - begin
-- NOT dangling one since it is in 'final' status (it's been processed)
insert into PAYMENT (payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'bac1ksqi26f9t2h7q3henmlamc',
 'SUCCESS',
 100,
 now() - interval '92 minutes',
 now() - interval '90 minutes'
);

insert into PAYMENT (payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'cac1ksqi26f9t2h7q3henmlamc',
 'FAILED',
 100,
 now() - interval '92 minutes',
 null
);

insert into PAYMENT (payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'dac1ksqi26f9t2h7q3henmlamc',
 'CANCELLED',
 100,
 now() - interval '92 minutes',
 null
);

insert into PAYMENT (payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'eac1ksqi26f9t2h7q3henmlamc',
 'ERROR',
 100,
 now() - interval '92 minutes',
 null
);
-------------- PAYMENTS in 'final' states - end


-------------- dangling ones

insert into PAYMENT (payment_id, payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('88c1b316-06b3-11ea-a9f4-2b3d49d48bfd',
 'CREDIT_DEBIT_CARD',
 'cancelled-payment-id',
 'CREATED',
 1000,
 now() - interval '92 minutes',
 null
);

insert into vehicle_entrant_payment (payment_id, vrn, caz_id, travel_date, charge_paid, payment_status)
values
(
    '88c1b316-06b3-11ea-a9f4-2b3d49d48bfd',
    'DS98UDG',
    '53e03a28-0627-11ea-9511-ffaaee87e375',
    now(),
    1000,
    'NOT_PAID'
);

----

insert into PAYMENT (payment_id, payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('6ff12efa-0615-11ea-9511-2771b85b976f',
 'CREDIT_DEBIT_CARD',
 'expired-payment-id',
 'INITIATED',
 1000,
 now() - interval '3 hours',
 null
);

insert into vehicle_entrant_payment (payment_id, vrn, caz_id, travel_date, charge_paid, payment_status)
values
(
    '6ff12efa-0615-11ea-9511-2771b85b976f',
    'OI64EFO',
    '53e03a28-0627-11ea-9511-ffaaee87e375',
    now(),
    1000,
    'NOT_PAID'
);
----

---- payment which was finished successfully on GOV UK side, but not on ours -- begin
insert into PAYMENT (payment_id, payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('3cefd184-0627-11ea-9511-87ff6d6f54ab',
 'CREDIT_DEBIT_CARD',
 'success-payment-id',
 'CREATED',
 1000,
 now() - interval '92 minutes',
 null
);

insert into vehicle_entrant_payment (payment_id, vrn, caz_id, travel_date, charge_paid, payment_status)
values
(
    '3cefd184-0627-11ea-9511-87ff6d6f54ab',
    'LE35LMK',
    '53e03a28-0627-11ea-9511-ffaaee87e375',
    now(),
    1000,
    'NOT_PAID'
);

insert into vehicle_entrant (vehicle_entrant_id, caz_entry_timestamp, caz_id, vrn, caz_entry_date)
values
(
    '6e8c947a-0627-11ea-9511-27edb54b6c81',
    now(),
    '53e03a28-0627-11ea-9511-ffaaee87e375',
    'LE35LMK',
    now()
);
---- payment which was finished successfully on GOV UK side, but not on ours -- end
