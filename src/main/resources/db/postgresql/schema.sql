create table if not exists fintech_ledger_entries (
    id text primary key,
    value_time timestamptz not null,
    booking_time timestamptz not null,
    settlement_time timestamptz,
    reason text not null default '',
    metadata_json jsonb not null default '{}'::jsonb
);

create table if not exists fintech_ledger_postings (
    entry_id text not null references fintech_ledger_entries(id),
    ordinal integer not null,
    account_id text not null,
    side text not null check (side in ('DEBIT', 'CREDIT')),
    amount text not null,
    asset_type text not null check (asset_type in ('FIAT', 'CRYPTO')),
    asset_code text not null,
    asset_scale integer not null check (asset_scale >= 0),
    asset_display_name text not null,
    asset_network text,
    asset_contract_address text,
    asset_symbol text,
    memo text not null default '',
    primary key (entry_id, ordinal),
    check (amount !~ '^-')
);

create table if not exists fintech_idempotency_records (
    domain text not null,
    operation text not null,
    actor text not null,
    key_value text not null,
    payload_hash text,
    result_text text,
    created_at timestamptz not null default now(),
    primary key (domain, operation, actor, key_value)
);

create table if not exists fintech_reservations (
    id text primary key,
    account_id text not null,
    status text not null check (status in ('HELD', 'SETTLED', 'RELEASED', 'EXPIRED')),
    created_at timestamptz not null,
    amount text not null,
    asset_type text not null check (asset_type in ('FIAT', 'CRYPTO')),
    asset_code text not null,
    asset_scale integer not null check (asset_scale >= 0),
    asset_display_name text not null,
    asset_network text,
    asset_contract_address text,
    asset_symbol text
);

create index if not exists fintech_reservations_held_account_idx
    on fintech_reservations(account_id, created_at, id)
    where status = 'HELD';

create table if not exists fintech_workflow_states (
    workflow_id text primary key,
    step_name text not null,
    completed boolean not null,
    data_json jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now()
);

create table if not exists fintech_raw_webhooks (
    id text primary key,
    provider_id text not null,
    received_at timestamptz not null,
    headers_json jsonb not null default '{}'::jsonb,
    raw_body bytea not null
);

create table if not exists fintech_outbox_events (
    id text primary key,
    topic text not null,
    payload text not null,
    created_at timestamptz not null,
    headers_json jsonb not null default '{}'::jsonb,
    published_at timestamptz
);

create index if not exists fintech_outbox_pending_idx
    on fintech_outbox_events(created_at, id)
    where published_at is null;

create table if not exists fintech_consumed_events (
    id text primary key,
    consumed_at timestamptz not null default now()
);

create table if not exists fintech_audit_events (
    id text primary key,
    occurred_at timestamptz not null,
    actor_type text not null,
    actor_id text not null,
    action text not null,
    reason_code text not null,
    reason_description text not null,
    data_json jsonb not null default '{}'::jsonb
);
