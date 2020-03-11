INSERT INTO caz_payment.t_clean_air_zone_entrant_payment(
  clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, payment_status, vehicle_entrant_captured, update_actor
) VALUES
  ('8501375c-37a4-11ea-a61e-efb4dbe2cde3', 'CAS123', '4dc6ea23-77d3-4bfe-8180-7662c33f88ad', '2020-01-13', 'NOT_PAID', 'FALSE', 'VCCS_API');
  INSERT INTO caz_payment.t_clean_air_zone_entrant_payment(
  clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, payment_status, vehicle_entrant_captured, update_actor
) VALUES
  ('6e7acc78-1081-46fa-92c5-1715a274dbb1', 'BCD234', '4dc6ea23-77d3-4bfe-8180-7662c33f88ad', to_date(to_char(current_timestamp, 'YYYY-MM-DD'),'YYYY-MM-DD'), 'PAID', 'TRUE', 'VCCS_API');