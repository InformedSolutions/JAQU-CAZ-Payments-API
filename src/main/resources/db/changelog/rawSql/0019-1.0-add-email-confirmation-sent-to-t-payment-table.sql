ALTER TABLE CAZ_PAYMENT.t_payment ADD COLUMN IF NOT EXISTS email_confirmation_sent boolean DEFAULT false NOT NULL;
