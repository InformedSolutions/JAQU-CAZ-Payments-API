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

insert into PAYMENT (payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'cancelled-payment-id',
 'CREATED',
 1000,
 now() - interval '92 minutes',
 null
);

insert into PAYMENT (payment_method, payment_provider_id,
                     payment_provider_status, total_paid, payment_submitted_timestamp,
                     payment_authorised_timestamp)
values
('CREDIT_DEBIT_CARD',
 'expired-payment-id',
 'INITIATED',
 1000,
 now() - interval '3 hours',
 null
);

