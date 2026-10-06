create table authorization_requests (
    id uuid primary key,
    state_hash text not null unique,
    client_id text not null,
    redirect_uri text not null,
    me text not null,
    client_state text,
    scope text not null default '',
    code_challenge text,
    code_challenge_method text,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null
);

create table authorization_codes (
    id uuid primary key,
    code_hash text not null unique,
    client_id text not null,
    redirect_uri text not null,
    me text not null,
    scope text not null default '',
    code_challenge text,
    code_challenge_method text,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null
);

create table access_tokens (
    id uuid primary key,
    token_hash text not null unique,
    me text not null,
    client_id text not null,
    scope text not null default '',
    issued_at timestamptz not null,
    expires_at timestamptz not null
);

create table refresh_tokens (
    id uuid primary key,
    token_hash text not null unique,
    me text not null,
    client_id text not null,
    scope text not null default '',
    issued_at timestamptz not null,
    expires_at timestamptz not null,
    used_at timestamptz
);

create index idx_authorization_request_expires_at on authorization_requests (expires_at);
create index idx_authorization_code_expires_at on authorization_codes (expires_at);
create index idx_access_token_expires_at on access_tokens (expires_at);
create index idx_access_token_me on access_tokens (me);
create index idx_refresh_token_expires_at on refresh_tokens (expires_at);
create index idx_refresh_token_me on refresh_tokens (me);
