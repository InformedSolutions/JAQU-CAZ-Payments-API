UPDATE caz_payment.t_clean_air_zone_entrant_payment
  SET update_actor = 'LA', payment_status = 'REFUNDED'
  WHERE vrn = 'ND84VSX' AND clean_air_zone_id = 'b8e53786-c5ca-426a-a701-b14ee74857d4' AND travel_date = '2019-11-01';

UPDATE caz_payment.t_clean_air_zone_entrant_payment
  SET update_actor = 'LA', payment_status = 'PAID'
  WHERE vrn = 'ND84VSX' AND clean_air_zone_id = 'b8e53786-c5ca-426a-a701-b14ee74857d4' AND travel_date = '2019-11-01';
