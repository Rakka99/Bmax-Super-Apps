-- 030_irisan_invoice_display_pln_mobile.sql
-- IRISAN invoice display follows the verified PLN Mobile checkout pattern:
-- current unpaid billing total + previous unpaid period penalty.
-- Do not alter billings rows or PREVENTIF/KOREKTIF calculations.

drop view if exists public.invoice_billing_display;

create view public.invoice_billing_display
with (security_invoker = true)
as
select
  b.id,
  b.external_reference as invoice_number,
  c.id_pelanggan as idpel,
  c.nama as customer_name,
  case
    when upper(coalesce(b.category,''))='IRISAN'
         and upper(coalesce(b.status,''))='UNPAID'
         and exists (
           select 1
           from public.billings p
           where p.customer_id=b.customer_id
             and p.period=to_char(to_date(b.period,'YYYYMM') - interval '1 month','YYYYMM')
             and upper(coalesce(p.status,''))='UNPAID'
         )
    then to_char(to_date(b.period,'YYYYMM') - interval '1 month','YYYYMM') || ',' || b.period
    else b.period
  end as period,
  b.amount as rptag_pln,
  0::numeric as admin_pln,
  b.penalty
    + case
        when upper(coalesce(b.category,''))='IRISAN'
             and upper(coalesce(b.status,''))='UNPAID'
        then coalesce((
          select p.penalty
          from public.billings p
          where p.customer_id=b.customer_id
            and p.period=to_char(to_date(b.period,'YYYYMM') - interval '1 month','YYYYMM')
            and upper(coalesce(p.status,''))='UNPAID'
          limit 1
        ),0)
        else 0
      end as penalty,
  case
    when upper(coalesce(b.category,''))='IRISAN'
         and upper(coalesce(b.status,''))='UNPAID'
    then coalesce(b.total_amount,0) + coalesce((
      select p.penalty
      from public.billings p
      where p.customer_id=b.customer_id
        and p.period=to_char(to_date(b.period,'YYYYMM') - interval '1 month','YYYYMM')
        and upper(coalesce(p.status,''))='UNPAID'
      limit 1
    ),0)
    else b.total_amount
  end as invoice_total,
  b.status,
  b.category,
  b.rbm_code,
  b.biller_id
from public.billings b
join public.customers c on c.id=b.customer_id;

comment on view public.invoice_billing_display is
'IRISAN customer-facing invoice display follows verified PLN Mobile pattern: current unpaid total plus prior unpaid penalty; previous RPTAG is not added again. PREVENTIF/KOREKTIF unchanged.';
