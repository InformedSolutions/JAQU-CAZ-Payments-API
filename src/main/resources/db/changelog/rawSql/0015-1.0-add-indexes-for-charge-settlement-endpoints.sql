CREATE INDEX IF NOT EXISTS entrant_payment_id ON caz_payment.t_clean_air_zone_entrant_payment_match (clean_air_zone_entrant_payment_id);

CREATE INDEX IF NOT EXISTS vrn ON caz_payment.t_clean_air_zone_entrant_payment (vrn);
CREATE INDEX IF NOT EXISTS travel_date ON caz_payment.t_clean_air_zone_entrant_payment (travel_date);
CREATE INDEX IF NOT EXISTS vrn_travel_date ON caz_payment.t_clean_air_zone_entrant_payment (vrn, travel_date);

CREATE INDEX IF NOT EXISTS travel_date_vrn_payment_status_caz_id ON caz_payment.t_clean_air_zone_entrant_payment (travel_date, vrn, payment_status, clean_air_zone_id);
CREATE INDEX IF NOT EXISTS caz_id_vrn_travel_date ON caz_payment.t_clean_air_zone_entrant_payment (clean_air_zone_id, vrn, travel_date);
