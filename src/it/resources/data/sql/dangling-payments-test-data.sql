--- This script inserts some dangling payments alongside with candidates


-- NOT dangling one since it was submitted less than 90 minutes ago
insert into CAZ_PAYMENT.T_PAYMENT (payment_method, payment_provider_id, payment_provider_status,
                                   total_paid, payment_submitted_timestamp,
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
insert into CAZ_PAYMENT.T_PAYMENT (payment_method, payment_provider_id, payment_provider_status,
                                   total_paid, payment_submitted_timestamp,
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
insert into CAZ_PAYMENT.T_PAYMENT (payment_method, payment_provider_id, payment_provider_status,
                                   total_paid, payment_submitted_timestamp,
                                   payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'bac1ksqi26f9t2h7q3henmlamc',
 'SUCCESS',
 100,
 now() - interval '92 minutes',
 now() - interval '90 minutes'
);

insert into CAZ_PAYMENT.T_PAYMENT (payment_method, payment_provider_id, payment_provider_status,
                                   total_paid, payment_submitted_timestamp,
                                   payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'cac1ksqi26f9t2h7q3henmlamc',
 'FAILED',
 100,
 now() - interval '92 minutes',
 null
);

insert into CAZ_PAYMENT.T_PAYMENT (payment_method, payment_provider_id, payment_provider_status,
                                   total_paid, payment_submitted_timestamp,
                                   payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'dac1ksqi26f9t2h7q3henmlamc',
 'CANCELLED',
 100,
 now() - interval '92 minutes',
 null
);

insert into CAZ_PAYMENT.T_PAYMENT (payment_method, payment_provider_id, payment_provider_status,
                                   total_paid, payment_submitted_timestamp,
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

insert into CAZ_PAYMENT.T_PAYMENT (payment_id, payment_method, payment_provider_id,
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

insert into CAZ_PAYMENT.T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT (clean_air_zone_entrant_payment_id, vrn,
  clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
(
    'c86a6dcd-a325-4e9b-bd80-b12aa0a1697a',
    'DS98UDG',
    '53e03a28-0627-11ea-9511-ffaaee87e375',
    now(),
    'TariffCode',
    1000,
    'NOT_PAID',
    'USER'
);

insert into CAZ_PAYMENT.T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_MATCH(clean_air_zone_entrant_payment_id,
                                                               payment_id, latest)
values
(
    'c86a6dcd-a325-4e9b-bd80-b12aa0a1697a',
    '88c1b316-06b3-11ea-a9f4-2b3d49d48bfd',
    true
);

----

insert into CAZ_PAYMENT.T_PAYMENT (payment_id, payment_method, payment_provider_id,
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

insert into CAZ_PAYMENT.T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT (clean_air_zone_entrant_payment_id, vrn,
  clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
(
    '2322e0e6-cbb2-4047-88a5-611ef935773b',
    'OI64EFO',
    '53e03a28-0627-11ea-9511-ffaaee87e375',
    now(),
    'TariffCode',
    500,
    'NOT_PAID',
    'USER'
),
(
    'f449bcdb-8cad-436b-a7cc-2ef9803ef2db',
    'OI64EFO',
    '53e03a28-0627-11ea-9511-ffaaee87e375',
    now() - interval '1 day',
    'TariffCode',
    500,
    'REFUNDED',
    'USER'
);
insert into CAZ_PAYMENT.T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_MATCH(clean_air_zone_entrant_payment_id,
                                                               payment_id, latest)
values
(
    '2322e0e6-cbb2-4047-88a5-611ef935773b',
    '6ff12efa-0615-11ea-9511-2771b85b976f',
    true
),
(
    'f449bcdb-8cad-436b-a7cc-2ef9803ef2db',
    '6ff12efa-0615-11ea-9511-2771b85b976f',
    true
)
;
----

---- payment which was finished successfully on GOV UK side, but not on ours -- begin
insert into CAZ_PAYMENT.T_PAYMENT (payment_id, payment_method, payment_provider_id,
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

insert into CAZ_PAYMENT.T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT (clean_air_zone_entrant_payment_id, vrn,
  clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
values
(
    'c8d47916-47f2-4572-b427-ae109c7cfbb8',
    'LE35LMK',
    '53e03a28-0627-11ea-9511-ffaaee87e375',
    now(),
    'TariffCode',
    1000,
    'NOT_PAID',
    'USER'
);
insert into CAZ_PAYMENT.T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT_MATCH(clean_air_zone_entrant_payment_id,
                                                               payment_id, latest)
values
(
    'c8d47916-47f2-4572-b427-ae109c7cfbb8',
    '3cefd184-0627-11ea-9511-87ff6d6f54ab',
    true
);
---- payment which was finished successfully on GOV UK side, but not on ours -- end
