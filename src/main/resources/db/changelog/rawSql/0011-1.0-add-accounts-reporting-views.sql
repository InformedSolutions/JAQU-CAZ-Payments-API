-- Add views for reporting on accounts payments

-- Number of successful accounts payments by day
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_daily AS
  SELECT date_trunc('day', payment_authorised_timestamp) AS day, 
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY day
  ORDER BY day;

-- Number of successful accounts payments by day and payment method
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_method_daily AS
  SELECT date_trunc('day', payment_authorised_timestamp) AS day,
  payment_method, 
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY day, payment_method
  ORDER BY day;

-- Number of unsuccessful accounts payments by day
CREATE OR REPLACE VIEW caz_reporting.unsuccessful_accounts_payments_daily AS
  SELECT date_trunc('day', insert_timestamp) AS day, 
  COUNT(*) as no_unsuccessful_payments
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status != 'SUCCESS'
  GROUP BY day
  ORDER BY day;

-- Number of successful accounts payments by day and caz
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_daily_by_caz AS  
  SELECT date_trunc('day', payment_authorised_timestamp) AS day, caz_name,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY day, caz_name
  ORDER BY day;

-- Number of successful accounts payments by day, payment method and caz
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_daily_by_caz_payment_method AS    
  SELECT date_trunc('day', payment_authorised_timestamp) AS day, caz_name,
  payment_method,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY day, caz_name, payment_method
  ORDER BY day;

-- Number of unsuccessful accounts payments by day and caz
CREATE OR REPLACE VIEW caz_reporting.unsuccessful_accounts_payments_daily_by_caz AS  
  SELECT date_trunc('day', pay.insert_timestamp) AS day, caz_name,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status != 'SUCCESS'
  GROUP BY day, caz_name
  ORDER BY day;
  
-- Number of successful accounts payments by month
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_monthly AS
  SELECT TO_CHAR(payment_authorised_timestamp, 'MM/YYYY') AS month, 
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY month
  ORDER BY month;

-- Number of successful accounts payments by month and payment method
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_method_monthly AS
  SELECT TO_CHAR(payment_authorised_timestamp, 'MM/YYYY') AS month,
  payment_method, 
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY month, payment_method
  ORDER BY month;

-- Number of unsuccessful accounts payments by month
CREATE OR REPLACE VIEW caz_reporting.unsuccessful_accounts_payments_monthly AS
  SELECT TO_CHAR(insert_timestamp, 'MM/YYYY') AS month, 
  COUNT(*) as no_unsuccessful_payments
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status != 'SUCCESS'
  GROUP BY month
  ORDER BY month;

-- Number of successful accounts payments by month and caz
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_monthly_by_caz AS
  SELECT TO_CHAR(payment_authorised_timestamp, 'MM/YYYY') AS month, caz_name,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY month, caz_name
  ORDER BY month;

-- Number of successful accounts payments by month, payment method and caz
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_monthly_method_caz AS  
  SELECT TO_CHAR(payment_authorised_timestamp, 'MM/YYYY') AS month, caz_name,
	payment_method,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY month, caz_name, payment_method
  ORDER BY month;

-- Number of unsuccessful accounts payments by month and caz
CREATE OR REPLACE VIEW caz_reporting.unsuccessful_accounts_payments_monthly_by_caz AS  
  SELECT TO_CHAR(pay.insert_timestamp, 'MM/YYYY') AS month, caz_name,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status != 'SUCCESS'
  GROUP BY month, caz_name
  ORDER BY month;
  
-- Number of successful accounts payments by week
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_weekly AS
  SELECT date_trunc('week', payment_authorised_timestamp) as week_commencing, 
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY week_commencing
  ORDER BY week_commencing;
  
-- Number of successful accounts payments by week and payment method
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_method_weekly AS
  SELECT date_trunc('week', payment_authorised_timestamp) AS week_commencing,
  payment_method, 
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY week_commencing, payment_method
  ORDER BY week_commencing;

-- Number of unsuccessful accounts payments by week
CREATE OR REPLACE VIEW caz_reporting.unsuccessful_accounts_payments_weekly AS
  SELECT date_trunc('week', insert_timestamp) AS week_commencing, 
  COUNT(*) as no_unsuccessful_payments
  FROM caz_payment.t_payment
  WHERE user_id IS NOT null 
  AND payment_provider_status != 'SUCCESS'
  GROUP BY week_commencing
  ORDER BY week_commencing;

-- Number of unsuccessful accounts payments by week and caz
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_weekly_by_caz AS
  SELECT date_trunc('week', payment_authorised_timestamp) AS week_commencing, caz_name,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY week_commencing, caz_name
  ORDER BY week_commencing;

-- Number of unsuccessful accounts payments by week, payment method and caz
CREATE OR REPLACE VIEW caz_reporting.successful_accounts_payments_weekly_method_caz AS  
  SELECT date_trunc('week', payment_authorised_timestamp) AS week_commencing, caz_name,
  payment_method,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status = 'SUCCESS'
  GROUP BY week_commencing, caz_name, payment_method
  ORDER BY week_commencing;

  -- Number of unsuccessful accounts payments by week and caz
CREATE OR REPLACE VIEW caz_reporting.unsuccessful_accounts_payments_weekly_by_caz AS 
  SELECT date_trunc('week', pay.insert_timestamp) AS week_commencing, caz_name,
  COUNT(*) AS no_successful_payments 
  FROM caz_payment.t_payment pay
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment_match AS pay_match
  on pay.payment_id = pay_match.payment_id
  INNER JOIN caz_payment.t_clean_air_zone_entrant_payment AS pay_entrant
  ON pay_entrant.clean_air_zone_entrant_payment_id = pay_match.clean_air_zone_entrant_payment_id
  INNER JOIN caz_reporting.t_clean_air_zone caz ON
  pay_entrant.clean_air_zone_id = caz.clean_air_zone_id
  WHERE user_id IS NOT null 
  AND payment_provider_status != 'SUCCESS'
  GROUP BY week_commencing, caz_name
  ORDER BY week_commencing;