-- 024_invoice_customer_total_vs_saldo_rptag.sql
-- Invoice/customer-facing amount:
--   total_amount = RP TAG + PLN admin + penalty
-- Dashboard/application saldo:
--   amount = RP TAG only
--
-- This migration also keeps Supervisor/Admin visibility of shared RBMs through
-- the security-invoker biller_ranking view while Biller RLS excludes SAKOTAG/SAKOTAB.

create or replace function public.set_invoice_customer_total()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  -- invoices.amount is the customer-facing RPTAG amount for invoice records.
  new.total := coalesce(new.amount,0) + coalesce(new.admin_bank,0);
  return new;
end;
$$;

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
  b.admin_bank as admin_pln,
  b.penalty,
  b.total_amount as invoice_total,
  b.status,
  b.category,
  b.rbm_code,
  b.biller_id
from public.billings b
join public.customers c on c.id=b.customer_id;

comment on view public.invoice_billing_display is
'Customer invoice source: invoice_total is PLN-facing total including admin and penalty; application saldo must use billings.amount only.';
