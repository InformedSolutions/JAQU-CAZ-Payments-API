CREATE SEQUENCE IF NOT EXISTS public.reference_number START WITH 1627;
ALTER TABLE public.payment ADD COLUMN central_reference_number bigint DEFAULT nextval('public.reference_number') NOT NULL;