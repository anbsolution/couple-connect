-- Couple Connect Phase 1 schema scaffold.
-- Apply only after creating a Supabase project and reviewing this migration.
create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null check (char_length(display_name) between 1 and 60),
  mobile_number text not null check (char_length(mobile_number) between 8 and 15),
  avatar_path text,
  created_at timestamptz not null default now()
);

create table if not exists public.pairing_codes (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  code_hash text not null unique,
  expires_at timestamptz not null,
  consumed_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.couples (
  id uuid primary key default gen_random_uuid(),
  partner_a uuid not null references auth.users(id) on delete cascade,
  partner_b uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint distinct_partners check (partner_a <> partner_b),
  constraint one_pair_order unique (partner_a, partner_b)
);

alter table public.profiles enable row level security;
alter table public.pairing_codes enable row level security;
alter table public.couples enable row level security;

-- Profiles are private to their owner at this starter stage.
create policy "profile owner can read" on public.profiles
for select to authenticated using (id = auth.uid());
create policy "profile owner can insert" on public.profiles
for insert to authenticated with check (id = auth.uid());
create policy "profile owner can update" on public.profiles
for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- Pairing codes and couples are intentionally inaccessible directly from clients.
-- Use a reviewed Edge Function with server-side authorization for pairing.
