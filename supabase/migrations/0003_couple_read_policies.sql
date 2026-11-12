create schema if not exists private;

create or replace function private.is_couple_partner(
  p_other_user_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.couple_members mine
    join public.couple_members theirs
      on theirs.couple_id = mine.couple_id
    where mine.user_id = auth.uid()
      and theirs.user_id = p_other_user_id
  );
$$;

revoke all on function private.is_couple_partner(uuid)
from public, anon;

grant usage on schema private to authenticated;

grant execute on function private.is_couple_partner(uuid)
to authenticated;

create policy "couple members can read own couple"
on public.couples
for select to authenticated
using (
  exists (
    select 1 from public.couple_members
    where couple_members.couple_id = couples.id
      and couple_members.user_id = auth.uid()
  )
);

create policy "users can read own membership"
on public.couple_members
for select to authenticated
using (user_id = auth.uid());

drop policy if exists "users can read their partner profile"
on public.profiles;

create policy "users can read their partner profile"
on public.profiles
for select to authenticated
using (
  id = auth.uid()
  or private.is_couple_partner(id)
);

drop function if exists public.is_couple_partner(uuid);
