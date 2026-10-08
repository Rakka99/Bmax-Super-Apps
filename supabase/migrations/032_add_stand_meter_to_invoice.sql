-- 032_add_stand_meter_to_invoice.sql
-- Persist PituCode stand meter readings and expose them to the invoice display.
-- Financial values and category rules are intentionally unchanged.

ALTER TABLE public.billings
  ADD COLUMN IF NOT EXISTS stand_meter text;

ALTER TABLE public.pitu_inquiry_audit
  ADD COLUMN IF NOT EXISTS stand_meter text;

COMMENT ON COLUMN public.billings.stand_meter IS
'PituCode PLN postpaid stand meter snapshot. Informational invoice field; never affects RPTAG, admin, penalty, or status.';

COMMENT ON COLUMN public.pitu_inquiry_audit.stand_meter IS
'Raw PituCode stand_meter captured from inquiry response for audit/invoice display.';

CREATE OR REPLACE VIEW public.invoice_billing_display AS
SELECT
  b.id,
  c.id_pelanggan AS idpel,
  COALESCE(c.nama, ''::text) AS customer_name,
  CASE
    WHEN (ia.provider_period_raw ~~* '%3 Bulan%'::text) THEN 'AGU26/SEP26/OKT26'::text
    WHEN (ia.provider_period_raw ~~* '%2 Bulan%'::text) THEN 'SEP26/OKT26'::text
    WHEN ((upper(COALESCE(b.category, ''::text)) = 'IRISAN'::text)
      AND (upper(COALESCE(b.status, ''::text)) = 'UNPAID'::text)
      AND (upper(COALESCE(p1.status, ''::text)) = 'UNPAID'::text)
      AND (upper(COALESCE(p2.status, ''::text)) = 'UNPAID'::text))
      THEN 'AGU26/SEP26/OKT26'::text
    WHEN ((upper(COALESCE(b.category, ''::text)) = 'IRISAN'::text)
      AND (upper(COALESCE(b.status, ''::text)) = 'UNPAID'::text)
      AND (upper(COALESCE(p1.status, ''::text)) = 'UNPAID'::text))
      THEN 'SEP26/OKT26'::text
    ELSE replace(
      replace(to_char((to_date(b.period, 'YYYYMM'::text))::timestamp with time zone, 'MONYY'::text),
        'AUG'::text, 'AGU'::text),
      'OCT'::text, 'OKT'::text)
  END AS period,
  b.status,
  b.category,
  COALESCE(b.amount, (0)::numeric) AS rptag_pln,
  COALESCE(b.admin_bank, (0)::numeric) AS admin_pln,
  COALESCE(b.penalty, (0)::numeric) AS penalty,
  (COALESCE(b.total_amount, (0)::numeric) + COALESCE(b.penalty, (0)::numeric)) AS invoice_total,
  b.rbm_code,
  b.biller_id,
  COALESCE(NULLIF(b.stand_meter, ''), ia.stand_meter) AS stand_meter,
  b.period AS billing_period
FROM public.billings b
JOIN public.customers c ON c.id = b.customer_id
LEFT JOIN public.billings p1
  ON p1.customer_id = b.customer_id
 AND p1.period = to_char((to_date(b.period, 'YYYYMM'::text) - '1 mon'::interval), 'YYYYMM'::text)
LEFT JOIN public.billings p2
  ON p2.customer_id = b.customer_id
 AND p2.period = to_char((to_date(b.period, 'YYYYMM'::text) - '2 mons'::interval), 'YYYYMM'::text)
LEFT JOIN LATERAL (
  SELECT a.provider_period_raw, a.stand_meter
  FROM public.pitu_inquiry_audit a
  WHERE a.idpel = c.id_pelanggan
    AND a.period = b.period
    AND a.validation_status = 'VALID'::text
  ORDER BY a.last_queried_at DESC
  LIMIT 1
) ia ON true;
