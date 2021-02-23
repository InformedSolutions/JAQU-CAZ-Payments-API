-- Add `email_confirmation_sent` attribute to T_PAYMENT table:
ALTER TABLE CAZ_PAYMENT.t_payment ADD COLUMN email_confirmation_sent boolean DEFAULT false NOT NULL;

-- Set email_confirmation_sent=true for already existing records:
UPDATE CAZ_PAYMENT.t_payment SET email_confirmation_sent = true;