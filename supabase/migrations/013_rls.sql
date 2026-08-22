alter table public.regions enable row level security;
alter table public.ulps enable row level security;
alter table public.profiles enable row level security;
alter table public.billers enable row level security;
alter table public.rbms enable row level security;
alter table public.customers enable row level security;
alter table public.billings enable row level security;
alter table public.inquiries enable row level security;
alter table public.payments enable row level security;
alter table public.invoices enable row level security;
alter table public.pdil_records enable row level security;
alter table public.audit_logs enable row level security;

create or replace function public.get_current_role()
returns public.user_role
language sql
security definer
set search_path = public
stable
as $$
  select role from public.profiles where id = auth.uid() and active = true;
$$;

create or replace function public.get_current_biller_id()
returns uuid
language sql
security definer
set search_path = public
stable
as $$
  select biller_id from public.profiles where id = auth.uid() and active = true;
$$;

create or replace function public.get_current_ulp_id()
returns uuid
language sql
security definer
set search_path = public
stable
as $$
  select ulp_id from public.profiles where id = auth.uid() and active = true;
$$;

revoke all on function public.get_current_role() from public, anon, authenticated;
revoke all on function public.get_current_biller_id() from public, anon, authenticated;
revoke all on function public.get_current_ulp_id() from public, anon, authenticated;

grant execute on function public.get_current_role() to authenticated;
grant execute on function public.get_current_biller_id() to authenticated;
grant execute on function public.get_current_ulp_id() to authenticated;

create policy profiles_self_or_admin on public.profiles
for select to authenticated
using (id = auth.uid() or public.get_current_role() = 'ADMIN');

create policy regions_scoped_read on public.regions
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or id = (select region_id from public.profiles where id = auth.uid())
);

create policy ulps_scoped_read on public.ulps
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or id = public.get_current_ulp_id()
);

create policy billers_scoped_read on public.billers
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or id = public.get_current_biller_id()
  or (public.get_current_role() = 'SUPERVISOR' and ulp_id = public.get_current_ulp_id())
);

create policy rbms_scoped_read on public.rbms
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or biller_id = public.get_current_biller_id()
  or (public.get_current_role() = 'SUPERVISOR' and ulp_id = public.get_current_ulp_id())
);

create policy customers_scoped_read on public.customers
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or biller_id = public.get_current_biller_id()
  or (public.get_current_role() = 'SUPERVISOR' and ulp_id = public.get_current_ulp_id())
);

create policy billings_scoped_read on public.billings
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or exists (
    select 1 from public.customers c
    where c.id = billings.customer_id
      and (
        c.biller_id = public.get_current_biller_id()
        or (public.get_current_role() = 'SUPERVISOR' and c.ulp_id = public.get_current_ulp_id())
      )
  )
);

create policy inquiries_scoped_read on public.inquiries
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or exists (
    select 1 from public.customers c
    where c.id = inquiries.customer_id
      and (
        c.biller_id = public.get_current_biller_id()
        or (public.get_current_role() = 'SUPERVISOR' and c.ulp_id = public.get_current_ulp_id())
      )
  )
);

create policy payments_scoped_read on public.payments
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or biller_id = public.get_current_biller_id()
  or (public.get_current_role() = 'SUPERVISOR' and exists (
    select 1 from public.billers b
    where b.id = payments.biller_id and b.ulp_id = public.get_current_ulp_id()
  ))
);

create policy invoices_scoped_read on public.invoices
for select to authenticated
using (
  public.get_current_role() = 'ADMIN'
  or exists (
    select 1 from public.customers c
    where c.id = invoices.customer_id
      and (
        c.biller_id = public.get_current_biller_id()
        or (public.get_current_role() = 'SUPERVISOR' and c.ulp_id = public.get_current_ulp_id())
      )
  )
);
