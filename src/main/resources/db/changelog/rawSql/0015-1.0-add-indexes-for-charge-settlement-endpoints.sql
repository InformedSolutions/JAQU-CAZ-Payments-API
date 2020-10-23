CREATE INDEX entrant_payment_id ON caz_payment.t_clean_air_zone_entrant_payment_match (clean_air_zone_entrant_payment_id);

CREATE INDEX vrn ON caz_payment.t_clean_air_zone_entrant_payment (vrn);
CREATE INDEX travel_date ON caz_payment.t_clean_air_zone_entrant_payment (travel_date);
CREATE INDEX vrn_travel_date ON caz_payment.t_clean_air_zone_entrant_payment (vrn, travel_date);

CREATE INDEX travel_date_vrn_payment_status_caz_id ON caz_payment.t_clean_air_zone_entrant_payment (travel_date, vrn, payment_status, clean_air_zone_id);
CREATE INDEX caz_id_vrn_travel_date ON caz_payment.t_clean_air_zone_entrant_payment (clean_air_zone_id, vrn, travel_date);