# Project agent memory

This file is the project's committed home for project-intrinsic agent knowledge: build, test, release, architecture, and
sharp-edge notes that should travel with the code.

- Add durable project-specific notes here as they are discovered through real work.

## Maintaining this file

Keep this file for knowledge useful to almost every future agent session in this project.
Do not repeat what the codebase already shows; point to the authoritative file or command instead.
Prefer rewriting or pruning existing entries over appending new ones.
When updating this file, preserve this bar for all agents and keep entries concise.

## What Sigil is

Sigil is the standalone IndieAuth provider for Jacob's site. It was extracted from Bastion, which is now a pure
resource server. Sigil has no local accounts: GitHub is the only identity provider, and the browser-facing auth UI is
delegated to the "Herald" (personal-site) service via `sigil.indieauth.herald.*`.

## Build and test

- `./gradlew test` runs the full suite; Spring Boot tests use Testcontainers (needs Docker).
- `./gradlew ktlintCheck` runs the linter; `./gradlew ktlintFormat` fixes style.
- Jackson 3 (`tools.jackson.*`) is used, not Jackson 2; `@JsonProperty` still comes from
  `com.fasterxml.jackson.annotation`.
- JobRunr: methods invoked from a scheduled/enqueued job lambda must not use Kotlin default parameter values -
  JobRunr fails to schedule them. Lambda args are also serialized once at registration and replayed on every fire,
  so never pass a computed time (e.g. `Instant.now()`) through the lambda - compute it inside the invoked method, as
  `IndieAuthRowPurgeService.purge()` does.

## IndieAuth

- Server metadata is served from `/.well-known/oauth-authorization-server` (plus the legacy `.well-known`
  endpoint probes) and advertises the `issuer` (trailing-slash normalized, https-only via `util/Issuers.kt`), the
  token/introspection/revocation/userinfo endpoints, and `authorization_response_iss_parameter_supported=true`. The
  metadata URL itself is discovered via the `indieauth-metadata` link relation published on the external `me` site;
  Sigil serves no profile pages.
- The authorization redirect carries `code` + `state` + `iss` (`util/Redirects.kt`); `iss` must equal the metadata
  issuer for mix-up protection.
- Code redemption is split per spec: POST `/indieauth/auth` returns `{me}` only (`ProfileUrlController` +
  `ProfileUrlService`), POST `/indieauth/token` returns tokens (`TokenController` + `AccessTokenService`) and rejects
  empty-scope codes with `invalid_grant`. Token responses carry `expires_in` and rotate a `refresh_token`
  (`grant_type=refresh_token`, same-or-narrower scope, single-use rotation via `RefreshTokenService`).
- Introspection (`/indieauth/introspect`, RFC 7662 plus `me`), revocation (`/indieauth/revocation`, always 200), and
  userinfo (`/indieauth/userinfo`) are served and advertised. Profile claims come from static
  `sigil.indieauth.profile.*` config (`ProfileClaimService`); `email` needs both `profile` and `email` scopes.
- Introspection accepts either an active Sigil-issued access token or the configured shared service token
  (`sigil.indieauth.service.token`, constant-time compared). Bastion uses the service token to validate Micropub
  bearer tokens remotely; it holds no token state of its own.
- Raw `state`/`code`/`access_token`/`refresh_token` values are never persisted - only their SHA-256 digests
  (`security/Tokens.kt`). PKCE is lenient for max client compat: a missing `code_challenge` is accepted with a
  warning, S256-only when present, and redemption enforces the conditional rule.
- Client validation (`util/IndieAuthUrls.kt`, `service/ClientMetadataFetcher.kt`) enforces strict profile/client URL
  rules and the 4.2.2 cross-host redirect allowlist (JSON `redirect_uris`, `Link rel=redirect_uri`, HTML link tags).
  Loopback clients are never fetched; inconclusive fetches allow with a warning (fail-open), a fetched allowlist
  missing the target blocks.
- Adding a provider = one new `IdentityProvider` implementation plus config.
