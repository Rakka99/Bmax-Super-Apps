create extension if not exists pgcrypto;

do $$ begin
  create type public.user_role as enum ('ADMIN','SUPERVISOR','BILLER');
exception when duplicate_object then null; end $$;

create table if not exists public.regions (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  name text not null,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.ulps (
  id uuid primary key default gen_random_uuid(),
  region_id uuid not null references public.regions(id),
  code text not null unique,
  name text not null,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  role public.user_role not null default 'BILLER',
  region_id uuid references public.regions(id),
  ulp_id uuid references public.ulps(id),
  biller_id uuid,
  phone text,
  avatar_url text,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.billers (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid unique references public.profiles(id),
  ulp_id uuid not null references public.ulps(id),
  code text not null unique,
  name text not null,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.profiles
  drop constraint if exists profiles_biller_id_fkey;

alter table public.profiles
  add constraint profiles_biller_id_fkey
  foreign key (biller_id) references public.billers(id);

create table if not exists public.rbms (
  id uuid primary key default gen_random_uuid(),
  biller_id uuid not null references public.billers(id),
  ulp_id uuid not null references public.ulps(id),
  code text not null check (code in ('A','B','C','D','E')),
  name text not null,
  sequence integer not null check (sequence between 1 and 5),
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (biller_id, code)
);

create table if not exists public.customers (
  id uuid primary key default gen_random_uuid(),
  idpel text not null unique,
  meter_number text,
  name text not null,
  phone text,
  address text,
  village text,
  district text,
  city text,
  postal_code text,
  tariff text,
  power_va integer,
  region_id uuid not null references public.regions(id),
  ulp_id uuid not null references public.ulps(id),
  biller_id uuid not null references public.billers(id),
  rbm_id uuid not null references public.rbms(id),
  rbm_code text not null check (rbm_code in ('A','B','C','D','E')),
  langkah integer,
  gardu text,
  tiang text,
  latitude double precision,
  longitude double precision,
  status text,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.billings (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.customers(id),
  period text not null,
  amount numeric not null default 0,
  admin_fee numeric not null default 0,
  penalty numeric not null default 0,
  total numeric not null default 0,
  due_date date,
  status text not null check (status in ('UNPAID','PENDING','PAID','FAILED')),
  category text check (category in ('PREVENTIF','KOREKTIF','IRISAN')),
  paid_at timestamptz,
  source text,
  source_ref_id text,
  raw_data jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(customer_id, period)
);

create table if not exists public.inquiries (
  id uuid primary key default gen_random_uuid(),
  ref_id text not null unique,
  customer_id uuid not null references public.customers(id),
  provider text not null default 'IAK',
  iak_tr_id integer,
  period text,
  amount numeric,
  admin_fee numeric,
  penalty numeric,
  total numeric,
  status text,
  response_code text,
  message text,
  raw_response jsonb,
  created_by uuid references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.payments (
  id uuid primary key default gen_random_uuid(),
  ref_id text not null unique,
  inquiry_id uuid references public.inquiries(id),
  customer_id uuid not null references public.customers(id),
  biller_id uuid not null references public.billers(id),
  rbm_id uuid not null references public.rbms(id),
  created_by uuid references auth.users(id),
  iak_tr_id integer unique,
  period text,
  amount numeric,
  admin_fee numeric,
  penalty numeric,
  total numeric,
  status text,
  response_code text,
  message text,
  raw_response jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.invoices (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.customers(id),
  payment_id uuid references public.payments(id),
  invoice_number text not null unique,
  total numeric not null default 0,
  issued_at timestamptz not null default now(),
  pdf_url text,
  created_at timestamptz not null default now()
);

create table if not exists public.pdil_records (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid references public.customers(id),
  period text,
  payload jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.audit_logs (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid references auth.users(id),
  action text not null,
  entity text,
  entity_id uuid,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
