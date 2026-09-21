-- 020_shared_supervisor_rbm_scope.sql
-- Shared RBMs SAKOTAG/SAKOTAB belong to Supervisor scope.
-- BILLER scope must exclude them from customers, billing, field tasks and operational aggregates.
-- ADMIN/SUPERVISOR/MANAGER scope remains unchanged.

create or replace function public.is_shared_supervisor_rbm(p_rbm_code text)
returns boolean
language sql
immutable
as $function$
  select upper(trim(coalesce(p_rbm_code,''))) in ('SAKOTAG','SAKOTAB');
$function$;

drop policy if exists customers_select_scoped on public.customers;
create policy customers_select_scoped on public.customers
for select to authenticated
using (
  (select current_profile_role()) = 'ADMIN'
  or (
    (select current_profile_role()) = 'SUPERVISOR'
    and ulp_id = (select current_profile_ulp_id())
  )
  or (
    (select current_profile_role()) = 'BILLER'
    and lower(trim(biller_id)) = lower(trim((select current_profile_biller_id())))
    and not public.is_shared_supervisor_rbm(rbm_code)
  )
);

drop policy if exists billings_select_scoped on public.billings;
create policy billings_select_scoped on public.billings
for select to authenticated
using (
  (select current_profile_role()) = 'ADMIN'
  or (
    (select current_profile_role()) = 'SUPERVISOR'
    and ulp_id = (select current_profile_ulp_id())
  )
  or (
    (select current_profile_role()) = 'BILLER'
    and lower(trim(biller_id)) = lower(trim((select current_profile_biller_id())))
    and not public.is_shared_supervisor_rbm(rbm_code)
  )
);

drop policy if exists "Biller Only Sees Own RBM" on public.rbm;
create policy "Biller Only Sees Own RBM" on public.rbm
for select to authenticated
using (
  (select current_profile_role()) in ('ADMIN','SUPERVISOR')
  or (
    lower(trim(biller_id)) = lower(trim((select current_profile_biller_id())))
    and not public.is_shared_supervisor_rbm(code)
  )
);

drop policy if exists rbm_select_scoped on public.rbm_routes;
create policy rbm_select_scoped on public.rbm_routes
for select to authenticated
using (
  private.is_manager()
  or (
    biller_id = private.current_user_biller_id()
    and not public.is_shared_supervisor_rbm(code)
  )
);

drop policy if exists field_tasks_select_scoped on public.field_tasks;
create policy field_tasks_select_scoped on public.field_tasks
for select to authenticated
using (
  (select current_profile_role()) in ('ADMIN','SUPERVISOR')
  or (
    assigned_to = (select auth.uid())
    and not exists (
      select 1
      from public.customers c
      where c.id_pelanggan = field_tasks.id_pelanggan
        and public.is_shared_supervisor_rbm(c.rbm_code)
    )
  )
);

drop policy if exists field_tasks_update_scoped on public.field_tasks;
create policy field_tasks_update_scoped on public.field_tasks
for update to authenticated
using (
  (select current_profile_role()) in ('ADMIN','SUPERVISOR')
  or (
    assigned_to = (select auth.uid())
    and not exists (
      select 1
      from public.customers c
      where c.id_pelanggan = field_tasks.id_pelanggan
        and public.is_shared_supervisor_rbm(c.rbm_code)
    )
  )
)
with check (
  (select current_profile_role()) in ('ADMIN','SUPERVISOR')
  or (
    assigned_to = (select auth.uid())
    and not exists (
      select 1
      from public.customers c
      where c.id_pelanggan = field_tasks.id_pelanggan
        and public.is_shared_supervisor_rbm(c.rbm_code)
    )
  )
);

do $migration$
declare
  v_def text;
begin
  -- Operational dashboard: exclude shared RBMs only for BILLER scope.
  select pg_get_functiondef('public.get_operational_dashboard_stats()'::regprocedure)
    into v_def;
  v_def := replace(
    v_def,
    'or (me.role=''BILLER'' and lower(trim(c.biller_id))=lower(trim(me.biller_id)))',
    'or (me.role=''BILLER'' and lower(trim(c.biller_id))=lower(trim(me.biller_id)) and not public.is_shared_supervisor_rbm(c.rbm_code))'
  );
  execute v_def;

  -- Category status: same BILLER scope exclusion.
  select pg_get_functiondef('public.get_operational_category_status(text)'::regprocedure)
    into v_def;
  v_def := replace(
    v_def,
    'or (me.role=''BILLER'' and lower(trim(c.biller_id))=lower(trim(me.biller_id)))',
    'or (me.role=''BILLER'' and lower(trim(c.biller_id))=lower(trim(me.biller_id)) and not public.is_shared_supervisor_rbm(c.rbm_code))'
  );
  execute v_def;

  -- Monitoring summary: exclude shared RBMs from current and previous BILLER scope.
  select pg_get_functiondef('public.get_billing_monitoring_summary(timestamptz,timestamptz,timestamptz,timestamptz)'::regprocedure)
    into v_def;
  v_def := replace(
    v_def,
    'OR (v_role=''BILLER'' AND lower(trim(c.biller_id))=lower(trim(v_biller_id)))',
    'OR (v_role=''BILLER'' AND lower(trim(c.biller_id))=lower(trim(v_biller_id)) AND NOT public.is_shared_supervisor_rbm(b.rbm_code))'
  );
  execute v_def;

  -- Operational report: exclude shared RBMs from BILLER scope.
  select pg_get_functiondef('public.get_operational_report(text)'::regprocedure)
    into v_def;
  v_def := replace(
    v_def,
    'where b.period=p_period and (v_role in (''ADMIN'',''SUPERVISOR'',''MANAGER'') or b.biller_id=v_biller_id)',
    'where b.period=p_period and (v_role in (''ADMIN'',''SUPERVISOR'',''MANAGER'') or (b.biller_id=v_biller_id and not public.is_shared_supervisor_rbm(b.rbm_code)))'
  );
  execute v_def;

  -- Operational customer search: hide shared RBM customers for BILLER.
  select pg_get_functiondef('public.search_operational_customers(text,text,integer,integer)'::regprocedure)
    into v_def;
  v_def := replace(
    v_def,
    'or (v_role = ''BILLER'' and lower(trim(c.biller_id)) = lower(trim(v_biller)))',
    'or (v_role = ''BILLER'' and lower(trim(c.biller_id)) = lower(trim(v_biller)) and not public.is_shared_supervisor_rbm(c.rbm_code))'
  );
  execute v_def;

  -- Operational rankings: BILLER sees only non-shared RBM billing.
  select pg_get_functiondef('public.get_operational_biller_rankings(text)'::regprocedure)
    into v_def;
  v_def := replace(
    v_def,
    'where exists(select 1 from public.profiles me where me.id=auth.uid() and me.active=true and (me.role in(''ADMIN'',''SUPERVISOR'') or b.biller_id=me.biller_id))',
    'where exists(select 1 from public.profiles me where me.id=auth.uid() and me.active=true and (me.role in(''ADMIN'',''SUPERVISOR'') or (b.biller_id=me.biller_id and not public.is_shared_supervisor_rbm(b.rbm_code))))'
  );
  execute v_def;

  -- Dashboard stats overload: exclude shared RBMs from BILLER scope, including payment totals linked to billing.
  select pg_get_functiondef('public.get_dashboard_stats(text,text)'::regprocedure)
    into v_def;
  v_def := replace(
    v_def,
    'and (scope_biller is null or b.biller_id=scope_biller);',
    'and (scope_biller is null or b.biller_id=scope_biller)
    and (r <> ''BILLER'' or not public.is_shared_supervisor_rbm(b.rbm_code));'
  );
  v_def := replace(
    v_def,
    'from public.payments p
  where p.status=''PAID''',
    'from public.payments p
  left join public.billings b on b.id=p.billing_id
  where p.status=''PAID'''
  );
  v_def := replace(
    v_def,
    'and (scope_biller is null or p.biller_id=scope_biller);',
    'and (scope_biller is null or p.biller_id=scope_biller)
    and (r <> ''BILLER'' or not public.is_shared_supervisor_rbm(coalesce(b.rbm_code, '''')));'
  );
  v_def := replace(
    v_def,
    'and (scope_biller is null or biller_id=scope_biller);',
    'and (scope_biller is null or biller_id=scope_biller)
    and (r <> ''BILLER'' or not public.is_shared_supervisor_rbm(rbm_code));'
  );
  execute v_def;
end
$migration$;
