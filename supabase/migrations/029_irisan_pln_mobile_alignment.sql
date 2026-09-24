-- 029_irisan_pln_mobile_alignment.sql
-- IRISAN is a two-month running billing category.
-- This source migration documents the production read-time fix:
--   IRISAN display = current unpaid total_amount + previous unpaid period penalty.
-- The previous period RPTAG is NOT added again.
-- PREVENTIF/KOREKTIF/general billing rows are not mutated by this migration.

create or replace function public.search_operational_customers(
  p_query text default '',
  p_category text default null,
  p_limit integer default 50,
  p_offset integer default 0
)
returns table(
  server_id uuid,
  id_pelanggan text,
  nama text,
  alamat text,
  tarif text,
  daya integer,
  no_hp text,
  status text,
  biller_id text,
  biller_name text,
  category text,
  bill_amount bigint,
  penalty_amount bigint,
  bill_period text,
  due_date date,
  is_paid boolean,
  latitude double precision,
  longitude double precision,
  rbm_code text
)
language plpgsql
stable
security definer
set search_path to ''
as $function$
declare
  v_role text;
  v_biller text;
  v_ulp text;
begin
  if (select auth.uid()) is null then
    raise exception 'Authentication required';
  end if;

  select p.role::text, p.biller_id, p.ulp_id
    into v_role, v_biller, v_ulp
  from public.profiles p
  where p.id = (select auth.uid())
    and p.active = true;

  if v_role is null then
    raise exception 'Profile not found';
  end if;

  if greatest(coalesce(p_limit, 50), 0) > 100 then
    raise exception 'Invalid page size';
  end if;

  return query
  with latest as (
    select distinct on (b.customer_id)
      b.customer_id,
      b.period,
      b.total_amount,
      b.admin_bank,
      b.penalty,
      b.status,
      b.category,
      b.due_date,
      b.biller_id,
      b.rbm_code
    from public.billings b
    order by b.customer_id, b.period desc, b.updated_at desc
  ),
  display_rows as (
    select
      c.id as base_customer_id,
      l.period,
      l.total_amount,
      l.admin_bank,
      l.penalty,
      l.status,
      l.category,
      l.due_date,
      l.biller_id,
      l.rbm_code,
      case
        when upper(coalesce(l.category,'')) = 'IRISAN'
             and upper(coalesce(l.status,'')) = 'UNPAID'
        then coalesce(l.total_amount,0) + coalesce((
          select x.penalty
          from public.billings x
          where x.customer_id = c.id
            and x.period = to_char(to_date(l.period,'YYYYMM') - interval '1 month','YYYYMM')
            and upper(coalesce(x.status,'')) = 'UNPAID'
          limit 1
        ),0)
        else coalesce(l.total_amount,0)
      end as display_total_amount,
      coalesce(l.penalty,0) as display_penalty_amount,
      case
        when upper(coalesce(l.category,'')) = 'IRISAN'
             and upper(coalesce(l.status,'')) = 'UNPAID'
             and exists (
               select 1
               from public.billings x
               where x.customer_id = c.id
                 and x.period = to_char(to_date(l.period,'YYYYMM') - interval '1 month','YYYYMM')
                 and upper(coalesce(x.status,'')) = 'UNPAID'
             )
        then to_char(to_date(l.period,'YYYYMM') - interval '1 month','YYYYMM') || ',' || l.period
        else l.period
      end as display_period
    from public.customers c
    left join latest l on l.customer_id = c.id
  )
  select
    c.id,
    c.id_pelanggan,
    c.nama,
    c.alamat,
    c.tarif,
    c.daya,
    c.no_hp,
    c.status,
    c.biller_id,
    coalesce(bm.name, p.full_name, c.biller_id, 'Biller'),
    coalesce(d.category, 'PREVENTIF'),
    coalesce(round(d.display_total_amount)::bigint, 0),
    coalesce(round(d.display_penalty_amount)::bigint, 0),
    d.display_period,
    d.due_date,
    (coalesce(d.status, 'UNPAID') = 'PAID'),
    case
      when c.koordinat is not null
      then extensions.st_y(c.koordinat::extensions.geometry)
      else null
    end,
    case
      when c.koordinat is not null
      then extensions.st_x(c.koordinat::extensions.geometry)
      else null
    end,
    coalesce(d.rbm_code, c.rbm_code)
  from public.customers c
  left join display_rows d on d.base_customer_id = c.id
  left join public.billers bm on bm.username = c.biller_id
  left join public.profiles p on p.biller_id = c.biller_id and p.active = true
  where
    (
      v_role = 'ADMIN'
      or (v_role = 'SUPERVISOR' and c.ulp_id = v_ulp)
      or (v_role = 'BILLER'
          and lower(trim(c.biller_id)) = lower(trim(v_biller))
          and not public.is_shared_supervisor_rbm(c.rbm_code))
    )
    and (
      nullif(trim(p_query), '') is null
      or c.id::text ilike '%' || trim(p_query) || '%'
      or c.id_pelanggan ilike '%' || trim(p_query) || '%'
      or c.nama ilike '%' || trim(p_query) || '%'
      or c.alamat ilike '%' || trim(p_query) || '%'
    )
    and (
      p_category is null
      or upper(coalesce(d.category, 'PREVENTIF')) = upper(p_category)
    )
  order by c.nama, c.id_pelanggan
  limit greatest(coalesce(p_limit, 50), 1)
  offset greatest(coalesce(p_offset, 0), 0);
end;
$function$;

create or replace function public.get_operational_category_status(
  p_period text default null
)
returns table(
  category text,
  total_count bigint,
  paid_count bigint,
  unpaid_count bigint,
  total_amount numeric,
  paid_amount numeric,
  unpaid_amount numeric
)
language sql
stable
security definer
set search_path to 'public'
as $function$
with me as (
  select
    upper(p.role::text) as role,
    coalesce(nullif(trim(p.biller_id), ''), nullif(trim(p.username), '')) as biller_id,
    p.ulp_id
  from public.profiles p
  where p.id=(select auth.uid()) and p.active=true
  limit 1
),
scoped as (
  select b.*
  from public.billings b
  join public.customers c on c.id=b.customer_id
  cross join me
  where (p_period is null or b.period=p_period)
    and (
      me.role='ADMIN'
      or (me.role='SUPERVISOR' and c.ulp_id=me.ulp_id)
      or (
        me.role='BILLER'
        and lower(trim(c.biller_id))=lower(trim(me.biller_id))
        and not public.is_shared_supervisor_rbm(b.rbm_code)
      )
    )
),
displayed as (
  select
    s.*,
    case
      when upper(coalesce(s.category,''))='IRISAN'
           and upper(coalesce(s.status,''))='UNPAID'
      then coalesce(s.total_amount,0) + coalesce((
        select x.penalty
        from public.billings x
        where x.customer_id=s.customer_id
          and x.period = to_char(to_date(s.period,'YYYYMM') - interval '1 month','YYYYMM')
          and upper(coalesce(x.status,''))='UNPAID'
        limit 1
      ),0)
      else s.total_amount
    end as display_total_amount
  from scoped s
)
select
  upper(coalesce(category,'PREVENTIF')) as category,
  count(*)::bigint as total_count,
  count(*) filter(where status='PAID')::bigint as paid_count,
  count(*) filter(where status='UNPAID')::bigint as unpaid_count,
  coalesce(sum(display_total_amount),0) as total_amount,
  coalesce(sum(display_total_amount) filter(where status='PAID'),0) as paid_amount,
  coalesce(sum(display_total_amount) filter(where status='UNPAID'),0) as unpaid_amount
from displayed
group by upper(coalesce(category,'PREVENTIF'))
order by upper(coalesce(category,'PREVENTIF'));
$function$;
