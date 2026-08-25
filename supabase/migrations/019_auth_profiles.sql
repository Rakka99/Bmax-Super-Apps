-- Keep Supabase Authentication and application authorization in sync.
-- Every newly registered Auth user receives a BILLER profile by default.
-- ADMIN/SUPERVISOR roles must be assigned by an administrator in the database.

create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, role, active)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', split_part(coalesce(new.email, ''), '@', 1)),
    'BILLER',
    true
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

-- Backfill profiles for users that existed before this migration.
insert into public.profiles (id, full_name, role, active)
select
  u.id,
  coalesce(u.raw_user_meta_data ->> 'full_name', split_part(coalesce(u.email, ''), '@', 1)),
  'BILLER'::public.user_role,
  true
from auth.users u
left join public.profiles p on p.id = u.id
where p.id is null;

-- Ensure authenticated users can read only their own profile unless they are ADMIN.
drop policy if exists profiles_self_or_admin on public.profiles;
create policy profiles_self_or_admin on public.profiles
for select to authenticated
using (id = auth.uid() or public.get_current_role() = 'ADMIN');
