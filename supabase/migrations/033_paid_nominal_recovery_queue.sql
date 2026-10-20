-- 033_paid_nominal_recovery_queue.sql
-- Allow confirmed PAID records without a verified nominal while keeping financial values explicit.
ALTER TABLE public.billings
  ALTER COLUMN amount DROP NOT NULL;

ALTER TABLE public.billings
  ADD COLUMN IF NOT EXISTS nominal_status text NOT NULL DEFAULT 'AVAILABLE',
  ADD COLUMN IF NOT EXISTS nominal_source text,
  ADD COLUMN IF NOT EXISTS nominal_checked_at timestamptz;

ALTER TABLE public.billings
  DROP CONSTRAINT IF EXISTS billings_nominal_status_check;
ALTER TABLE public.billings
  ADD CONSTRAINT billings_nominal_status_check
  CHECK (nominal_status = ANY (ARRAY['AVAILABLE','PENDING','UNAVAILABLE']));

ALTER TABLE public.billings
  DROP CONSTRAINT IF EXISTS billings_amount_null_requires_paid_check;
ALTER TABLE public.billings
  ADD CONSTRAINT billings_amount_null_requires_paid_check
  CHECK (amount IS NOT NULL OR (status = 'PAID' AND nominal_status = 'UNAVAILABLE'));

UPDATE public.billings
SET nominal_status = CASE WHEN upper(coalesce(status,'')) = 'PAID' THEN 'UNAVAILABLE' ELSE 'PENDING' END,
    nominal_source = NULL
WHERE amount IS NULL OR amount <= 0;

CREATE OR REPLACE FUNCTION private.sync_billing_nominal_metadata()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
  IF NEW.amount IS NOT NULL AND NEW.amount > 0 THEN
    NEW.nominal_status := 'AVAILABLE';
    IF NULLIF(btrim(NEW.nominal_source), '') IS NULL THEN
      NEW.nominal_source := CASE upper(coalesce(NEW.source, ''))
        WHEN 'API' THEN 'PITU_PROVIDER'
        WHEN 'IMPORT' THEN 'BILLING_IMPORT'
        WHEN 'MANUAL' THEN 'MANUAL_ENTRY'
        ELSE 'TRUSTED_SOURCE'
      END;
    END IF;
  ELSIF upper(coalesce(NEW.status, '')) = 'PAID' THEN
    NEW.nominal_status := 'UNAVAILABLE';
    NEW.nominal_source := NULL;
  ELSE
    NEW.nominal_status := 'PENDING';
    NEW.nominal_source := NULL;
  END IF;

  IF NEW.amount IS NULL AND upper(coalesce(NEW.status, '')) <> 'PAID' THEN
    RAISE EXCEPTION 'Billing amount may be NULL only for confirmed PAID billing';
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_billing_nominal_metadata ON public.billings;
CREATE TRIGGER trg_sync_billing_nominal_metadata
BEFORE INSERT OR UPDATE OF amount, status, source, nominal_source, nominal_status
ON public.billings
FOR EACH ROW EXECUTE FUNCTION private.sync_billing_nominal_metadata();

COMMENT ON COLUMN public.billings.nominal_status IS
'AVAILABLE means a positive validated amount exists; PENDING means unpaid amount needs validation; UNAVAILABLE means payment is confirmed but nominal is not currently known.';
COMMENT ON COLUMN public.billings.nominal_source IS
'Source of validated nominal, e.g. PITU_PROVIDER, BILLING_MASTER_STAGING, BILLING_IMPORT_ROWS, MANUAL_ENTRY, TRUSTED_SOURCE. NULL means no valid nominal is known.';
COMMENT ON COLUMN public.billings.nominal_checked_at IS
'Last attempt to recover or validate billing nominal.';

CREATE TABLE IF NOT EXISTS public.pitu_paid_nominal_recovery_queue (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  period text NOT NULL CHECK (period ~ '^[0-9]{6}$'),
  idpel text NOT NULL,
  customer_id uuid NOT NULL REFERENCES public.customers(id) ON DELETE CASCADE,
  biller_id text,
  billing_id uuid REFERENCES public.billings(id) ON DELETE SET NULL,
  status text NOT NULL DEFAULT 'PENDING'
    CHECK (status = ANY (ARRAY['PENDING','PROCESSING','RESOLVED','DEAD'])),
  attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
  next_retry_at timestamptz NOT NULL DEFAULT now(),
  last_attempt_at timestamptz,
  last_error text,
  resolution_source text,
  resolved_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT pitu_paid_nominal_recovery_queue_period_idpel_key UNIQUE (period, idpel)
);

CREATE INDEX IF NOT EXISTS idx_pitu_paid_nominal_recovery_due
  ON public.pitu_paid_nominal_recovery_queue (next_retry_at, created_at)
  WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_pitu_paid_nominal_recovery_biller
  ON public.pitu_paid_nominal_recovery_queue (biller_id, period, status);

ALTER TABLE public.pitu_paid_nominal_recovery_queue ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.pitu_paid_nominal_recovery_queue FROM PUBLIC, anon, authenticated;
GRANT ALL ON TABLE public.pitu_paid_nominal_recovery_queue TO service_role;

COMMENT ON TABLE public.pitu_paid_nominal_recovery_queue IS
'Separate retry lane for confirmed PAID bills with unknown nominal; must not block sequential biller sync cursor.';

CREATE OR REPLACE VIEW public.invoice_billing_display
WITH (security_invoker = true)
AS
SELECT
  b.id,
  c.id_pelanggan AS idpel,
  COALESCE(c.nama, ''::text) AS customer_name,
  CASE
    WHEN ia.provider_period_raw ~~* '%3 Bulan%' THEN 'AGU26/SEP26/OKT26'
    WHEN ia.provider_period_raw ~~* '%2 Bulan%' THEN 'SEP26/OKT26'
    WHEN upper(COALESCE(b.category, '')) = 'IRISAN'
      AND upper(COALESCE(b.status, '')) = 'UNPAID'
      AND upper(COALESCE(p1.status, '')) = 'UNPAID'
      AND upper(COALESCE(p2.status, '')) = 'UNPAID' THEN 'AGU26/SEP26/OKT26'
    WHEN upper(COALESCE(b.category, '')) = 'IRISAN'
      AND upper(COALESCE(b.status, '')) = 'UNPAID'
      AND upper(COALESCE(p1.status, '')) = 'UNPAID' THEN 'SEP26/OKT26'
    ELSE replace(replace(to_char(to_date(b.period, 'YYYYMM')::timestamptz, 'MONYY'), 'AUG', 'AGU'), 'OCT', 'OKT')
  END AS period,
  b.status,
  b.category,
  CASE WHEN b.nominal_status = 'AVAILABLE' THEN b.amount ELSE NULL::numeric END AS rptag_pln,
  CASE WHEN b.nominal_status = 'AVAILABLE' THEN COALESCE(b.admin_bank, 0::numeric) ELSE 0::numeric END AS admin_pln,
  COALESCE(b.penalty, 0::numeric) AS penalty,
  CASE WHEN b.nominal_status = 'AVAILABLE'
    THEN COALESCE(b.total_amount, 0::numeric) + COALESCE(b.penalty, 0::numeric)
    ELSE NULL::numeric
  END AS invoice_total,
  b.rbm_code,
  b.biller_id,
  COALESCE(NULLIF(b.stand_meter, ''), ia.stand_meter) AS stand_meter,
  b.period AS billing_period,
  b.nominal_status,
  b.nominal_source
FROM public.billings b
JOIN public.customers c ON c.id = b.customer_id
LEFT JOIN public.billings p1
  ON p1.customer_id = b.customer_id
 AND p1.period = to_char(to_date(b.period, 'YYYYMM') - interval '1 month', 'YYYYMM')
LEFT JOIN public.billings p2
  ON p2.customer_id = b.customer_id
 AND p2.period = to_char(to_date(b.period, 'YYYYMM') - interval '2 months', 'YYYYMM')
LEFT JOIN LATERAL (
  SELECT a.provider_period_raw, a.stand_meter
  FROM public.pitu_inquiry_audit a
  WHERE a.idpel = c.id_pelanggan
    AND a.period = b.period
    AND a.validation_status IN ('VALID','PAID_NO_AMOUNT','PAID')
  ORDER BY a.last_queried_at DESC
  LIMIT 1
) ia ON true;

COMMENT ON VIEW public.invoice_billing_display IS
'Invoice view returns NULL nominal and total for confirmed PAID rows with nominal_status UNAVAILABLE; clients should display Nominal belum tersedia, not Rp0.';
