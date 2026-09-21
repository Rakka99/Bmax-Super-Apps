-- 023_saldo_rptag_only_all_roles.sql
-- Application saldo is RP TAG PLN only (amount).
-- Excludes PLN admin, Biller fee, and penalty from displayed saldo/monitoring.
-- Customer-facing billings.total_amount remains amount + admin_bank + penalty.

do $$
declare v_def text;
begin
  select pg_get_functiondef('public.get_operational_dashboard_stats()'::regprocedure) into v_def;
  v_def := replace(v_def,'(coalesce(sa.amount,0)+coalesce(sa.penalty,0)) as effective_total','coalesce(sa.amount,0) as effective_total');
  execute v_def;

  select pg_get_functiondef('public.get_operational_category_status(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'sum(amount+coalesce(penalty,0))','sum(amount)');
  execute v_def;

  select pg_get_functiondef('public.get_billing_dashboard(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'sum(amount+coalesce(penalty,0))','sum(amount)');
  execute v_def;

  select pg_get_functiondef('public.get_dashboard_stats(text,text)'::regprocedure) into v_def;
  v_def := replace(v_def,'sum(b.amount+coalesce(b.penalty,0))','sum(b.amount)');
  execute v_def;

  select pg_get_functiondef('public.get_billing_monitoring_summary(timestamptz,timestamptz,timestamptz,timestamptz)'::regprocedure) into v_def;
  v_def := replace(v_def,'sum(amount+coalesce(penalty,0))','sum(amount)');
  execute v_def;

  select pg_get_functiondef('public.get_operational_report(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'sum(amount+coalesce(penalty,0))','sum(amount)');
  execute v_def;

  select pg_get_functiondef('public.get_operational_biller_rankings(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'sum(b.amount+coalesce(b.penalty,0))','sum(b.amount)');
  execute v_def;

  select pg_get_functiondef('public.get_biller_collection_ranking(text)'::regprocedure) into v_def;
  v_def := replace(v_def,'SUM(b.amount+coalesce(b.penalty,0))','SUM(b.amount)');
  execute v_def;

  select pg_get_functiondef('public.get_billing_daily_trend(timestamptz,timestamptz)'::regprocedure) into v_def;
  v_def := replace(v_def,'SUM(total_amount)','SUM(amount)');
  v_def := replace(v_def,
    'OR (me.role=''BILLER'' AND lower(trim(c.biller_id))=lower(trim(me.biller_id)))',
    'OR (me.role=''BILLER'' AND lower(trim(c.biller_id))=lower(trim(me.biller_id)) AND NOT public.is_shared_supervisor_rbm(b.rbm_code))'
  );
  execute v_def;

  select pg_get_functiondef('public.get_billing_monthly_summary(text,text)'::regprocedure) into v_def;
  v_def := replace(v_def,'COALESCE(sum(b.total_amount),0)','COALESCE(sum(b.amount),0)');
  v_def := replace(v_def,'COALESCE(sum(b.total_amount) FILTER (WHERE b.status=''PAID''),0)','COALESCE(sum(b.amount) FILTER (WHERE b.status=''PAID''),0)');
  execute v_def;

  select pg_get_functiondef('public.get_dashboard_stats()'::regprocedure) into v_def;
  v_def := replace(v_def,'coalesce(sum(total_amount),0)','coalesce(sum(amount),0)');
  execute v_def;
end $$;
