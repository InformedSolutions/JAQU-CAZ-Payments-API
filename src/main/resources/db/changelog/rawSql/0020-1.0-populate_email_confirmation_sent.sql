-- Disables all triggers for the current session
SET session_replication_role = replica;

-- Set email_confirmation_sent=true for already existing records:
UPDATE CAZ_PAYMENT.t_payment SET email_confirmation_sent = true;

-- Enables triggers back
SET session_replication_role = DEFAULT;