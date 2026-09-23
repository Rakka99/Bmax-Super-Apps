-- 025_fix_202609_pln_mobile_total_formula.sql
-- 202609 customer-facing total must follow the validated PLN billing structure:
--   total_amount = RPTAG + actual penalty
-- Dashboard saldo remains RPTAG only (billings.amount).
-- admin_bank is preserved as source/import metadata and is NOT added again
-- as a PLN Mobile fee.

alter table public.billings drop column if exists total_amount;

alter table public.billings
  add column total_amount numeric
  generated always as (
    coalesce(amount,0) + coalesce(penalty,0)
  ) stored;

create or replace function public.pln_customer_total(
  p_rptag numeric,
  p_period text default null,
  p_penalty numeric default 0
)
returns numeric
language sql
immutable
set search_path = ''
as $function$
  select greatest(coalesce(p_rptag,0),0)
       + greatest(coalesce(p_penalty,0),0);
$function$;

create or replace function public.set_invoice_customer_total()
returns trigger
language plpgsql
set search_path = public
as $function$
declare
  v_penalty numeric := 0;
begin
  select coalesce(b.penalty,0)
    into v_penalty
  from public.billings b
  where b.customer_id = new.customer_id
    and b.period = new.period
  order by b.updated_at desc
  limit 1;

  new.total := public.pln_customer_total(new.amount, new.period, v_penalty);
  return new;
end;
$function$;

drop trigger if exists trg_invoice_customer_total on public.invoices;
create trigger trg_invoice_customer_total
before insert or update of amount, admin_bank on public.invoices
for each row execute function public.set_invoice_customer_total();

drop view if exists public.invoice_billing_display;
create view public.invoice_billing_display
with (security_invoker = true)
as
select
  b.id,
  b.external_reference as invoice_number,
  c.id_pelanggan as idpel,
  c.nama as customer_name,
  b.period,
  b.amount as rptag_pln,
  0::numeric as admin_pln,
  b.penalty,
  b.total_amount as invoice_total,
  b.status,
  b.category,
  b.rbm_code,
  b.biller_id
from public.billings b
join public.customers c on c.id=b.customer_id;

comment on view public.invoice_billing_display is
'202609 customer-facing total is RPTAG + actual penalty; billings.amount remains RPTAG-only dashboard saldo; admin_bank is preserved metadata and is not added again.';
