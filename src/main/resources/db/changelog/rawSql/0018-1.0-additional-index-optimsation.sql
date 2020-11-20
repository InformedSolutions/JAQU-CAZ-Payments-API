CREATE INDEX IF NOT EXISTS payment_id ON caz_payment.t_clean_air_zone_entrant_payment_match(payment_id);
CREATE INDEX IF NOT EXISTS payment_provider_status ON caz_payment.t_payment(payment_provider_status);
CREATE INDEX IF NOT EXISTS user_id ON caz_payment.t_payment(user_id);