create table if not exists public.couple_members (
  user_id uuid primary key references auth.users(id) on delete cascade,
  couple_id uuid not null references public.couples(id) on delete cascade,
  joined_at timestamptz not null default now()
);

alter table public.couple_members enable row level security;

insert into public.couple_members (user_id, couple_id)
select partner_a, id from public.couples
on conflict (user_id) do nothing;

insert into public.couple_members (user_id, couple_id)
select partner_b, id from public.couples
on conflict (user_id) do nothing;

create index if not exists pairing_codes_owner_created_idx
on public.pairing_codes (owner_id, created_at desc);

create index if not exists pairing_codes_expiry_idx
on public.pairing_codes (expires_at);

create or replace function public.create_pairing_invite(
  p_owner_id uuid,
  p_code_hash text,
  p_expires_at timestamptz
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if p_owner_id is null or p_code_hash is null
     or length(p_code_hash) <> 64 then
    raise exception 'invalid_request';
  end if;

  if p_expires_at <= now()
     or p_expires_at > now() + interval '5 minutes 30 seconds' then
    raise exception 'invalid_expiry';
  end if;

  if exists (
    select 1 from public.couple_members
    where user_id = p_owner_id
  ) then
    raise exception 'already_paired';
  end if;

  if (
    select count(*) from public.pairing_codes
    where owner_id = p_owner_id
      and created_at > now() - interval '1 minute'
  ) >= 3 then
    raise exception 'rate_limited';
  end if;

  insert into public.pairing_codes
    (owner_id, code_hash, expires_at)
  values (p_owner_id, p_code_hash, p_expires_at);
end;
$$;

create or replace function public.accept_pairing_invite(
  p_user_id uuid,
  p_code_hash text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_invite public.pairing_codes%rowtype;
  v_couple_id uuid;
  v_a uuid;
  v_b uuid;
begin
  if p_user_id is null or p_code_hash is null
     or length(p_code_hash) <> 64 then
    raise exception 'invalid_request';
  end if;

  select * into v_invite
  from public.pairing_codes
  where code_hash = p_code_hash
  for update;

  if not found or v_invite.consumed_at is not null
     or v_invite.expires_at <= now() then
    raise exception 'invalid_or_expired_invite';
  end if;

  if v_invite.owner_id = p_user_id then
    raise exception 'cannot_pair_with_self';
  end if;

  if exists (
    select 1 from public.couple_members
    where user_id in (p_user_id, v_invite.owner_id)
  ) then
    raise exception 'already_paired';
  end if;

  v_a := least(p_user_id, v_invite.owner_id);
  v_b := greatest(p_user_id, v_invite.owner_id);

  insert into public.couples (partner_a, partner_b)
  values (v_a, v_b)
  returning id into v_couple_id;

  insert into public.couple_members (user_id, couple_id)
  values (v_a, v_couple_id), (v_b, v_couple_id);

  update public.pairing_codes
  set consumed_at = now()
  where id = v_invite.id;

  return v_couple_id;
end;
$$;

create or replace function public.revoke_pairing_invites(
  p_owner_id uuid
)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_count integer;
begin
  update public.pairing_codes
  set consumed_at = now()
  where owner_id = p_owner_id
    and consumed_at is null;

  get diagnostics v_count = row_count;
  return v_count;
end;
$$;

revoke all on function public.create_pairing_invite(uuid,text,timestamptz)
from public, anon, authenticated;

revoke all on function public.accept_pairing_invite(uuid,text)
from public, anon, authenticated;

revoke all on function public.revoke_pairing_invites(uuid)
from public, anon, authenticated;

grant execute on function public.create_pairing_invite(uuid,text,timestamptz)
to service_role;

grant execute on function public.accept_pairing_invite(uuid,text)
to service_role;

grant execute on function public.revoke_pairing_invites(uuid)
to service_role;
