-- Starter hosted schema. Run in a new Supabase project only after reviewing it.
create extension if not exists pgcrypto;

create table if not exists public.ai_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  assistant_name text not null default 'CallAI',
  language text not null default 'Tamil + English',
  greeting text not null default 'Hello! I am an AI assistant. How can I help you?',
  enabled boolean not null default true,
  handle_unanswered boolean not null default true,
  handle_busy boolean not null default true,
  updated_at timestamptz not null default now()
);
create table if not exists public.calls (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  provider_call_id text,
  caller_name text,
  caller_number text,
  purpose text,
  status text not null default 'completed',
  forwarding_reason text,
  duration_seconds integer not null default 0,
  callback_requested boolean not null default false,
  callback_time text,
  summary text,
  started_at timestamptz not null default now(),
  unique(user_id, provider_call_id)
);
create table if not exists public.transcripts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  call_id uuid not null references public.calls(id) on delete cascade,
  speaker text not null check (speaker in ('caller','assistant')),
  message text not null,
  created_at timestamptz not null default now()
);
create table if not exists public.recordings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  call_id uuid not null references public.calls(id) on delete cascade,
  storage_path text not null,
  consent_status text not null default 'unknown',
  created_at timestamptz not null default now()
);
create table if not exists public.followups (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  call_id uuid references public.calls(id) on delete set null,
  reminder_at timestamptz,
  status text not null default 'pending',
  notes text
);

alter table public.ai_settings enable row level security;
alter table public.calls enable row level security;
alter table public.transcripts enable row level security;
alter table public.recordings enable row level security;
alter table public.followups enable row level security;

create policy "settings owner only" on public.ai_settings for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "calls owner only" on public.calls for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "transcripts owner only" on public.transcripts for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "recordings owner only" on public.recordings for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "followups owner only" on public.followups for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- Create a PRIVATE Supabase Storage bucket named `call-recordings` manually.
-- Never expose service-role keys or public recording URLs in the Android app.
