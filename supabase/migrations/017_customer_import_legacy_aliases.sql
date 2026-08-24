-- Compatibility aliases for older Bmax/Sheets column names.
-- Canonical live fields remain tariff and power_va.

alter table public.customer_import_staging
  add column if not exists tarif text generated always as (tariff) stored,
  add column if not exists daya integer generated always as (power_va) stored,
  add column if not exists gardu text;

alter table public.customers
  add column if not exists gardu text;

-- Keep the import RPC aligned with the additional legacy gardu field.
create or replace function public.import_customer_staging(p_batch_id uuid default null)
returns table(imported bigint, updated bigint, skipped bigint, invalid bigint)
language plpgsql
security definer
set search_path = public
as $$
declare
  r record;
  v_profile_id uuid;
  v_rbm_id uuid;
  v_customer_id uuid;
  v_inserted boolean;
  v_code text;
  v_sequence smallint;
  v_segment text;
begin
  imported := 0; updated := 0; skipped := 0; invalid := 0;

  for r in
    select * from public.customer_import_staging
    where p_batch_id is null or import_batch_id = p_batch_id
    order by created_at, id
  loop
    if nullif(trim(r.idpel), '') is null or nullif(trim(r.name), '') is null then
      invalid := invalid + 1;
      continue;
    end if;

    select p.id into v_profile_id
    from public.profiles p
    where lower(coalesce(p.full_name, '')) in (
      lower(coalesce(r.username, '')),
      lower(coalesce(r.nama_bil, ''))
    )
    order by case when lower(coalesce(p.full_name, '')) = lower(coalesce(r.username, '')) then 0 else 1 end
    limit 1;

    if v_profile_id is null then
      invalid := invalid + 1;
      continue;
    end if;

    v_code := upper(trim(coalesce(r.rbm, '')));
    if v_code not in ('A','B','C','D','E') then v_code := null; end if;

    v_rbm_id := null;
    if v_code is not null and nullif(trim(r.region), '') is not null and nullif(trim(r.ulp), '') is not null then
      v_sequence := case v_code when 'A' then 1 when 'B' then 2 when 'C' then 3 when 'D' then 4 when 'E' then 5 end;
      insert into public.bmax_rbms (biller_user_id, region, ulp, code, name, sequence_no, active)
      values (v_profile_id, trim(r.region), trim(r.ulp), v_code, 'RBM ' || v_code, v_sequence, true)
      on conflict (biller_user_id, code) do update set
        region = excluded.region,
        ulp = excluded.ulp,
        name = excluded.name,
        sequence_no = excluded.sequence_no,
        active = true
      returning id into v_rbm_id;
    else
      select b.id into v_rbm_id
      from public.bmax_rbms b
      where b.biller_user_id = v_profile_id and b.code = v_code
      limit 1;
    end if;

    v_segment := upper(coalesce(nullif(trim(r.segment), ''), 'PREVENTIF'));
    if v_segment not in ('PREVENTIF','IRISAN','KOREKTIF') then v_segment := 'PREVENTIF'; end if;

    insert into public.customers (
      idpel, meter_number, name, phone, address, latitude, longitude,
      tariff, power_va, ulp, rbm, region, status, current_bill,
      arrears_total, segment, notes, assigned_biller, created_at, updated_at,
      bmax_rbm_id, bmax_biller_id, username, nama_bil, kode_petugas, tiang, gardu
    ) values (
      trim(r.idpel), nullif(trim(r.meter_number), ''), trim(r.name),
      nullif(trim(r.phone), ''), nullif(trim(r.address), ''), r.latitude, r.longitude,
      nullif(trim(r.tariff), ''), r.power_va, nullif(trim(r.ulp), ''),
      nullif(trim(r.rbm), ''), nullif(trim(r.region), ''), nullif(trim(r.status), ''),
      0, 0, v_segment, nullif(trim(r.kode_petugas), ''), v_profile_id, now(), now(),
      v_rbm_id, v_profile_id, nullif(trim(r.username), ''), nullif(trim(r.nama_bil), ''),
      nullif(trim(r.kode_petugas), ''), nullif(trim(r.tiang), ''), nullif(trim(r.gardu), '')
    )
    on conflict (idpel) do update set
      meter_number = excluded.meter_number,
      name = excluded.name,
      phone = excluded.phone,
      address = excluded.address,
      latitude = excluded.latitude,
      longitude = excluded.longitude,
      tariff = excluded.tariff,
      power_va = excluded.power_va,
      ulp = excluded.ulp,
      rbm = excluded.rbm,
      region = excluded.region,
      status = excluded.status,
      segment = excluded.segment,
      notes = excluded.notes,
      assigned_biller = excluded.assigned_biller,
      updated_at = now(),
      bmax_rbm_id = excluded.bmax_rbm_id,
      bmax_biller_id = excluded.bmax_biller_id,
      username = excluded.username,
      nama_bil = excluded.nama_bil,
      kode_petugas = excluded.kode_petugas,
      tiang = excluded.tiang,
      gardu = excluded.gardu
    returning id, (xmax = 0) into v_customer_id, v_inserted;

    if v_inserted then imported := imported + 1; else updated := updated + 1; end if;
  end loop;

  return next;
end;
$$;

revoke all on function public.import_customer_staging(uuid) from public, anon;
grant execute on function public.import_customer_staging(uuid) to authenticated;
