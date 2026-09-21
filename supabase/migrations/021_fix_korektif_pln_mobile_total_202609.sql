-- 021_fix_korektif_pln_mobile_total_202609.sql
-- Customer-facing billing total follows PLN Mobile:
--   total_amount = amount + admin_bank + penalty
-- Biller monitoring balance remains:
--   biller_total_amount = amount + penalty
--
-- 202609 Korektif priority rows are reconciled to billing data:
-- category, rbm_code, amount, admin_bank, penalty, total_amount, due_date.

drop view if exists public.biller_ranking;

alter table public.billings
  add column if not exists biller_total_amount numeric
  generated always as (amount + coalesce(penalty,0)) stored;

alter table public.billings drop column if exists total_amount;

alter table public.billings
  add column total_amount numeric
  generated always as (amount + coalesce(admin_bank,0) + coalesce(penalty,0)) stored;

update public.billing_master_staging m
set amount = b.amount,
    admin_bank = b.admin_bank,
    penalty = coalesce(b.penalty,0),
    total_amount = b.amount + coalesce(b.admin_bank,0) + coalesce(b.penalty,0),
    category = b.category,
    rbm_code = b.rbm_code,
    due_date = b.due_date
from public.billings b
join public.customers c on c.id=b.customer_id
where b.period='202609'
  and b.category='KOREKTIF'
  and b.biller_id in (
    '53511.wildan','53511.danikuswandi','53511.risnapebrian','53511.sandi',
    '53511.master','53511.rubihidayat','53511.syarifhidayat','53511.elirohyana',
    '53511.ceppytaufik','53511.dedendian','53511.nanoromansyah','53511.bangbang.w'
  )
  and m.period='202609'
  and m.idpel=c.id_pelanggan;

create view public.biller_ranking as
with current_period as (
  select coalesce(max(billings.period), '202609'::text) as period from public.billings
), base as (
  select b0.id,b0.username,b0.name,b0.code,b0.active,cp.period,
         count(bl.id) billing_count,
         count(bl.id) filter(where bl.status='PAID') paid_count,
         count(bl.id) filter(where bl.status='UNPAID') unpaid_count,
         coalesce(sum(bl.biller_total_amount),0) total_amount,
         coalesce(sum(bl.biller_total_amount) filter(where bl.status='PAID'),0) paid_amount,
         coalesce(sum(bl.biller_total_amount) filter(where bl.status='UNPAID'),0) unpaid_amount,
         count(bl.id) filter(where bl.category='PREVENTIF') preventive_count,
         count(bl.id) filter(where bl.category='KOREKTIF') corrective_count,
         count(bl.id) filter(where bl.category='IRISAN') intersection_count
  from public.billers b0 cross join current_period cp
  left join public.billings bl
    on lower(trim(bl.biller_id))=lower(trim(b0.username)) and bl.period=cp.period
  where b0.active=true
  group by b0.id,b0.username,b0.name,b0.code,b0.active,cp.period
)
select row_number() over(order by
          case when billing_count>0 then paid_count::numeric/billing_count::numeric else 0 end desc,
          paid_count desc,billing_count desc,paid_amount desc,upper(trim(name)),username)::integer rank,
       id,username,name,code,active,period,billing_count,paid_count,unpaid_count,
       round(case when billing_count>0 then paid_count::numeric*100/billing_count::numeric else 0 end,2) payment_rate_pct,
       preventive_count,corrective_count,intersection_count,total_amount,paid_amount,unpaid_amount
from base;

-- Preserve the dashboard/report functions while making all Biller-side monetary
-- aggregation use amount + penalty instead of the PLN admin-inclusive total_amount.
do $$
declare v_def text;
begin
  select pg_get_functiondef('public.get_operational_dashboard_stats()'::regprocedure) into v_def;
  v_def := replace(v_def,'coalesce(sa.total_amount,0) as effective_total','(coalesce(sa.amount,0)+coalesce(sa.penalty,0)) as effective_total');
  execute v_def;

  select pg_get_functiondef('public.get_operational_category_status(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'coalesce(sum(total_amount),0)','coalesce(sum(amount+coalesce(penalty,0)),0)');
  v_def := replace(v_def,'coalesce(sum(total_amount) filter(where status=''PAID''),0)','coalesce(sum(amount+coalesce(penalty,0)) filter(where status=''PAID''),0)');
  v_def := replace(v_def,'coalesce(sum(total_amount) filter(where status=''UNPAID''),0)','coalesce(sum(amount+coalesce(penalty,0)) filter(where status=''UNPAID''),0)');
  execute v_def;

  select pg_get_functiondef('public.get_billing_dashboard(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'coalesce(sum(total_amount), 0)','coalesce(sum(amount+coalesce(penalty,0)), 0)');
  v_def := replace(v_def,'coalesce(sum(total_amount) filter (where status = ''PAID''), 0)','coalesce(sum(amount+coalesce(penalty,0)) filter (where status = ''PAID''), 0)');
  v_def := replace(v_def,'coalesce(sum(total_amount) filter (where status = ''UNPAID''), 0)','coalesce(sum(amount+coalesce(penalty,0)) filter (where status = ''UNPAID''), 0)');
  v_def := replace(v_def,'sum(total_amount)','sum(amount+coalesce(penalty,0))');
  execute v_def;

  select pg_get_functiondef('public.get_dashboard_stats(text,text)'::regprocedure) into v_def;
  v_def := replace(v_def,'coalesce(sum(b.total_amount),0)','coalesce(sum(b.amount+coalesce(b.penalty,0)),0)');
  v_def := replace(v_def,'select coalesce(sum(p.total_amount),0) into paid_amount','select coalesce(sum(b.amount+coalesce(b.penalty,0)),0) into paid_amount');
  v_def := replace(v_def,'from public.payments p
  left join public.billings b on b.id=p.billing_id
  where p.status=''PAID''','from public.payments p
  join public.billings b on b.id=p.billing_id
  where p.status=''PAID''');
  execute v_def;

  select pg_get_functiondef('public.get_biller_collection_ranking(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'SUM(b.total_amount)','SUM(b.amount+coalesce(b.penalty,0))');
  v_def := replace(v_def,'SUM(b.total_amount) FILTER(WHERE b.status=''PAID'')','SUM(b.amount+coalesce(b.penalty,0)) FILTER(WHERE b.status=''PAID'')');
  execute v_def;

  select pg_get_functiondef('public.get_billing_monitoring_summary(timestamptz,timestamptz,timestamptz,timestamptz)'::regprocedure) into v_def;
  v_def := replace(v_def,'sum(total_amount)','sum(amount+coalesce(penalty,0))');
  v_def := replace(v_def,'SUM(total_amount)','SUM(amount+coalesce(penalty,0))');
  execute v_def;

  select pg_get_functiondef('public.get_operational_report(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'coalesce(sum(total_amount),0)','coalesce(sum(amount+coalesce(penalty,0)),0)');
  v_def := replace(v_def,'coalesce(sum(total_amount) filter(where upper(coalesce(status,''''))=''PAID''),0)','coalesce(sum(amount+coalesce(penalty,0)) filter(where upper(coalesce(status,''''))=''PAID''),0)');
  v_def := replace(v_def,'coalesce(sum(total_amount) filter(where upper(coalesce(status,''''))<>''PAID''),0)','coalesce(sum(amount+coalesce(penalty,0)) filter(where upper(coalesce(status,''''))<>''PAID''),0)');
  execute v_def;
end $$;
