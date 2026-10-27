# POS2 native mobile auth v1 — WORK / proposal only

Android Issue [#4](https://github.com/arioguswara-oss/Sanro-Android-Public/issues/4),
backend Issue [#15](https://github.com/arioguswara-oss/Sanro-POS-Stock/issues/15).
Date: 2026-10-28 WIB. No backend implementation is authorized by this proposal.
No routes below are claimed deployed. Android activation remains locked.

## Baseline and source findings

Android remote head matches `c705ed39f4d4d28c6093dbf81de88981be1c788d`.
Latest backend integration branch `codex/sanro-public-release-dev-20261024` was
resolved to `1e17f3267b94a22c5574abc06d712e4e2d62f429` (24 October).
Main is `61e95928340bc5879759ec43ad96a971797117f3`; main lacks the inspected web/API
runtime composition. The proposal targets the integration branch, not an assumed
main deployment. `source-evidence.json` records exact source hashes and main blob
identifiers. No authenticated live probe or hosting configuration was obtained;
prior read-only live GET evidence in the Android audit remains the only hosting
evidence. Public JS is not a deployment manifest. Get redacted startup path,
revision/digest, proxy routing and applicable authority response schema before coding.

Source paths below are within `v2-login-register/`, except the cloud guard:

| Source / verified behavior | Reuse or gap |
| --- | --- |
| `http/login-handlers.js`: POST email/password, activity/logout opaque bearer; recovery `confirm_revoke` | Existing private API, no device proof, refresh or lease |
| `lib/pos-login-service.js`: password, verified email, registration/ST binding, tenant/product/subscription gates | Reuse identity checks; cannot directly use `login()` for expired read-only because resolveGates throws `SUBSCRIPTION_EXPIRED` |
| Same service: default session 12 hours, stale threshold 15 minutes | Session TTL/heartbeat is **not** offline entitlement |
| `lib/mysql-session-store.js`: account row lock, unique token hash; active lookup scoped account+tenant | Protects one session per account, not one operational device across multiple accounts in the tenant |
| `sql/004_pos_v2_sessions.sql` | No device key, lease epoch, refresh family, tenant operational slot or recovery reservation |
| Recovery revokes healthy session on confirmation, stale session without it | Unsafe for offline cashier: offline is not abandoned; mobile must never infer transfer from inactivity |
| Session creation replaces expired sessions | Token expiry must not release mobile slot while an issued offline lease could still allow writes |
| `lib/pos-free-activation-service.js` | Reuses central idempotent Free activation, server `free_started_at/free_expires_at`; no device activation; missing dates currently allowed as null |
| `lib/access-context-client.js` | Server-only authority key, HTTPS, redirect denial; currently requests tenant access context only (ignores supplied productAccountId). Adapter must validate tenant/product binding and minimum authority schema rather than forwarding arbitrary `body.data` |
| `lib/pos-operational-access-gate.js` | Also rejects EXPIRED; lacks separate historical read permission |
| `runtime/web-server.js` | Browser cookie/CSRF; removes bearer from JSON. Preserve protections, no spoofed Origin or WebView cookie extraction |
| `runtime/api-server.js` | Separate private bearer candidate; public deployment unverified; do not expose wholesale |
| `operational-public/authenticated-web-host.js` | `plan !== PRO` -> `CLOUD_PLAN_DENIED`; preserve this guard and trusted DIRECT_CLOUD policy |

Existing source is **insufficient** for safe native offline activation. Proposed
new mobile adapter + device/session storage + entitlement evaluator + lease issuer
are required; no need for Free operational data in Cloud. Shared password/account/
tenant lookup, registration/activation and authority clients can be reused behind
strict interfaces. Refactor shared gates only in an approved implementation PR;
do not weaken legacy browser guards or mutate Superadmin as a side effect.

## Fixed decisions — no further approval requested

- Three-month Free Trial per new tenant, dates issued by platform; device login,
  reinstall, recovery and retries never restart or extend trial. No Android
  90-day calculation. Use central authoritative dates and calendar policy.
- Expiry (trial or paid) means READ_ONLY: legitimate users may login, view history,
  stock/reports and export within role permissions. All operational mutations
  denied; records retained. Expired does not mean unauthenticated or deleted.
- Free single operational device; initial activation online; Room/SQLite is
  operational authority and must work offline within a valid approved lease.
- No Free cloud operational writes or cloud transaction sync. Auth/entitlement
  control-plane traffic is distinct. No billing/checkout integration here.
- ST is internal storage identity; SR immutable display code. Neither is derived.
- Bluetooth, cutter, local DB and existing receipts are protected; no signing/AAB.

## Proposed native protocol (not an OAuth deployment claim)

New versioned namespace `/api/mobile/v1`, official isolated HTTPS staging origin
only; OpenAPI uses `.invalid` deliberately. Production routing is a separate gate.
This adapter activates FREE only. Recognized STARTER/PRO identities can authenticate
for role-filtered READ_ONLY history; their operational runtimes are not enabled by
this Free Android contract. Paid expiry still grants no writes. Unknown plans fail.
Native JSON credential login returns opaque, device-key-constrained access/refresh
credentials. Device proofs use the DPoP verification profile in
[RFC 9449](https://www.rfc-editor.org/rfc/rfc9449), ES256 P-256 only, public JWK
thumbprint per [RFC 7638](https://www.rfc-editor.org/rfc/rfc7638).
This does not claim the existing backend implements OAuth or a password grant.
Use a reviewed JOSE library; no custom ECDSA/DER encoding in business code.

Android creates nonexportable installation key in AndroidKeyStore. Client UUID or
Android ID is a label, not authority. Server derives thumbprint from valid proof,
issues opaque device_id and binds account, tenant, session and lease epoch.
All privileged requests require `Authorization: DPoP <access_token>` AND DPoP
proof; copied tokens without key fail. No Bearer downgrade. Refresh requires same
key, current refresh token and server nonce. Login requires credentials and proof
of proposed installation key. Proofs validate alg/typ/public-only JWK/signature,
exact HTTPS method/target, iat window, jti replay cache, server nonce and ath when
an access token is presented. Reject private JWK fields, unknown critical headers,
remote key URLs, `none`, redirects and arbitrary issuer/keys. Nonces are bounded,
one-use and consumed atomically with the command. Idempotent retry uses a fresh
proof/nonce and same request key. Clock bootstrap uses challenge server_time;
never treats phone wall clock as authorization.

Proposed staging engineering defaults (not Owner business policy): nonce 120s,
proof window 60s, access token 5 minutes, refresh idle/absolute limits 24h/30d,
16KiB JSON requests/64KiB responses, distributed credential limiter initially
5 failed attempts/minute per normalized account hash and 20/minute per IP with
progressive backoff; challenge 30/min/IP, authenticated 60/min/session. Shared
storage across workers, generic failures, no raw email in limiter/log keys.
Return 429 Retry-After; verify NAT/load behavior in staging. Refresh validity is
not permission to sell. Expiry cannot be extended by refreshing access tokens.

| Proposed operation | Input | Result / side effect |
| --- | --- | --- |
| POST `/challenges` | purpose, public JWK | nonce, challenge_id, expiry, server_time; no account disclosure |
| POST `/login` | email, password, challenge_id; DPoP | TokenPair + validated context. Authenticated, no offline write rights yet. EXPIRED yields READ_ONLY, not 402 |
| GET `/session` | access token + proof/nonce | Revalidated context; no tokens, local data or automatic lease |
| POST `/refresh` | refresh_token, challenge_id; proof | Atomic refresh rotation; same device, fresh context, no lease extension by itself |
| POST `/activation` | challenge_id, Idempotency-Key; token+proof | Acquires tenant operational slot, server device binding, signed offline write lease |
| POST `/leases` | same authentication/idempotency | Renews only currently authorized same binding, records max issued expiry before signing/reply |
| POST `/recovery` | credentials, challenge_id; new key proof | Creates recovery_id after reauthentication; target binding captured server-side; no revocation or grant yet |
| POST `/recovery/confirm` | recovery_id, confirm_revoke=true, challenge_id; same key proof | Consumes recovery, advances epoch/revokes credentials, reserves transfer and returns effective_after; no write lease or tokens |
| POST `/logout` | challenge_id; token+proof | Revokes token family, no further leases. Keeps slot reserved through outstanding lease maximum |

Recovery initiation and confirmation require idempotency keys, as do token login/
refresh to handle lost responses. Server stores request digest, identity/key,
outcome and bounded encrypted token response (never raw password) for 10 minutes;
same key + same authenticated request returns same result, different request
409 IDEMPOTENCY_CONFLICT. Validate identity/proof before replay. Exact consumed
refresh replay is allowed only for that bound idempotent response; otherwise revoke
family on reuse. Device serializes refresh and durably swaps ciphertext atomically.
After replay window, lost token response requires reauthentication; no fake success.

## Response/authorization semantics

`openapi.json` specifies required fields and errors; all fields are required
unless explicitly nullable. Unknown enum/version, missing binding/date/permission,
invalid signature or inconsistent identity -> locked writes. Error envelope:
`{ok:false, request_id, error:{code,retryable,action}}`; no credential or SQL echo.
No password/token in URLs, logs, traces or issue evidence. Cache-Control no-store.

Context includes account_id, product_account_id, ST+SR, server device_id and key
thumbprint, session_id, authorization_epoch, authority_revision, statuses, plan,
trial/subscription timestamps, server_time, storage_mode=LOCAL_ONLY,
access_mode=READ_WRITE or READ_ONLY, permissions and history_permissions.
Trial dates are mandatory for FREE (nullable for paid); expiry is exclusive. No Android fallbacks.
`authenticated=true` is a verified identity, not permission to write. `READ_WRITE`
means eligible to request a lease; local writes still require a valid signed lease.
EXPIRED has empty write permissions, no write lease, role-filtered history rights.
Suspension/inactive/unknown authority status denies fresh authorization; retained
local data is never wiped. Unknown future statuses cannot become READ_ONLY success.
No mobile route accepts tenant_id, plan or permission as caller-granted authority.

Lease: compact JWS per [RFC 7515](https://www.rfc-editor.org/rfc/rfc7515), protected
header typ=`sanro-pos-offline-lease+jwt`, alg=ES256, pinned/allowlisted kid.
Signed claims defined in OpenAPI `LeaseClaims`: iss, aud (official Android app
identity), sub/account/product/tenant/device/session, cnf.jkt, epoch, authority
revision, plan FREE, LOCAL_ONLY, permissions, history_permissions, iat/nbf/exp,
trial and entitlement expiration. Client verifies signature/issuer/audience/key
binding and `exp <= entitlement_expires_at`. Effective permissions are intersection
of trusted context, signed lease, local role capabilities; never client plan flags.
Staging has distinct issuer/audience/signing keys; never accepted by public app.
Key rotation manifest must be signed by pinned trust root, bounded/cached, reject
unknown/retired kid; old key verification retained for outstanding lease duration.
Signing/DB failure issues no unrecorded lease; max lease expiry recorded before
reply (conservative reservation is safe after lost response).

## Single device, offline time and recovery

Lock key for Free operational slot is `(tenant_id, product=POS)`, not email.
Retain existing account-session checks; distinct accounts cannot create a second
operational device. Browser/authenticated reads do not own another operational
slot. Every mobile issuer/refresh/recovery path coordinates on the same durable
slot row with epoch CAS/row lock. Missing mobile ledger cannot silently fall back
to old sessions. Existing browser recovery cannot release that mobile slot.

Lease duration L and revocation latency R require Owner policy selection. Fail-
closed default before selection: **no offline write lease issued**. After approval,
`exp = min(server_now + L, authoritative_entitlement_end)`; outages do not extend it.
Unknown network state does not erase a previously verified lease; known revocation
locks immediately. Offline revocation cannot be instantaneous. Phone reboot or
loss of trusted time anchor requires online revalidation for writes (conservative
proposal); force-close in the same boot can restore an intact binding/lease.

Within a boot, deadline uses trusted server time + elapsedRealtime, not wall time.
Persist boot identity, monotonic anchor, minimum accepted epoch/revision and signed
lease in protected local storage. Reject rollback, different boot, missing anchor,
tampering, negative elapsed time or restored backup. Revalidate inside serialized
Room transaction at commit boundary; in-flight expiry rolls back stock/sale/cart.
This protects a normal uncompromised Android installation, not a rooted attacker
who can replace the app or roll back all protected state. Stronger anti-rollback
would require online checks or proven secure hardware counters; not presumed.

Recommended strict transfer: reauthenticate + explicit confirm, stop renewal on
old binding and revoke online credentials; new binding waits until MAX outstanding
old lease expiry (even if old token expired, was logged out, or heartbeat stale).
Confirmation returns `effective_after`; new login then activation must recheck
current rights/epoch at/after that instant. Repeated/competing recovery cannot move
the reservation or silently steal it; recovery_id binds target epoch and new key,
expires in 10 minutes, then reauthentication is required. A new transfer must not
extend or resurrect old leases. Server time governs all release decisions.
Immediate transfer with old device offline allows overlap; it is not compatible
with a strict single operational device guarantee. The fixed single-device rule therefore requires waiting; it is not reopened
here. Owner selects lease duration/revocation tolerance, which determines the
maximum wait. No hidden force-transfer override is permitted.

Tokens are memory-only access + AES-GCM-encrypted refresh under Keystore key,
private noBackupFilesDir, fresh IV and AAD binding identity/version. Lease and time
anchors are integrity protected. Backup is disabled; database survives local logout/
expiry/key invalidation, but reinstall/uninstall cannot promise DB recovery.
No Cloud transaction backup is introduced. Session recovery restores authorization,
not lost SQLite data. Local export/import/replacement of device data needs a
separate scoped design and approval. Existing sandbox data never becomes live data.

## Android components and backend work packages after approval

Backend: versioned adapter, shared identity/auth evaluator (separate read/write
outcomes), strict authority DTO, device registry/tenant slot/epoch, refresh-family
hash store, nonce/replay/idempotency store, recovery reservation, offline lease
signer/key publication, audit/limiters. Proposed additive staging tables only;
review FK/index/locking/migrations and rollback separately. No auto-migration on
startup. Signer keys and authority credentials stay server-only; registration
continues to own three-month trial creation, never the mobile adapter.

Android: mockable native transport/parser; login/verification/recovery state machine;
Keystore token/device key storage; signature/time verifier; tenant-bound Room
repository; read-only history/export and write gates; connection status showing
account/license/local DB (no cloud sync); regression checks for atomic checkout,
restart, printer/cutter. No operational DB schema changes until approved migration
plan. Future core features are not restricted just because plan is Free.

## Staging and remaining Owner decisions

Staging must be explicitly assigned: isolated host/database/authority fixtures,
mail sink and nonproduction accounts A/B in tenant X and account C in tenant Y,
two device keys, controllable server clock and two workers. No Tunas Baru data,
production cookies, database or service secrets. Turn mobile routes off by default;
limit egress to staging authority; confirm expiry/context revisions with backend
operator. Run `acceptance.feature` against real staged HTTP + disposable MySQL,
not memory doubles alone; then Android fixture integration + physical printer UAT.
Record redacted revision/digest, test IDs and artifacts. No production probe can
substitute for concurrent/restart/crypto tests.

Only genuinely open policy decisions:
1. Offline lease maximum L and acceptable revocation delay R; operational impact
   of strict wait through old lease expiry on lost-device recovery.
2. Access/retention on old device after logout/transfer/security suspension (expiry
   read-only itself is already settled). Proposed safer default: lock until same
   identity reauthenticates; do not delete records.
3. Dedicated staging environment and fixture account authorization.
4. Owner approval to implement backend adapter in a separate code PR. This docs
   PR approves neither backend coding nor deployment.

Verification performed: five existing source test files passed under Node 22
(login, activity, operational gate, access client, Free activation). These do not
prove mobile behavior, production interoperability, real MySQL races or staging
acceptance. OpenAPI/fixture structural validation results are in VALIDATION.md.
Docs-only changes need no Android APK rebuild; runtime remains c705ed3 and its
previous 14 passing Android tests/Debug artifact are not new mobile test results.

## Error/status and semantic validation supplement

401: AUTH_INVALID, DEVICE_PROOF_INVALID, NONCE_REQUIRED, TOKEN_EXPIRED,
REFRESH_REUSED. 403: identity/security suspension, inactive subscription,
DEVICE_BINDING_MISMATCH, PLAN_UNSUPPORTED, READ_ONLY. 409: incomplete activation,
ACTIVE_SESSION_EXISTS, DEVICE_RECOVERY_REQUIRED, TRANSFER_PENDING,
RECOVERY_STALE, IDEMPOTENCY_CONFLICT. 400 VALIDATION_ERROR; 429 TOO_MANY_ATTEMPTS;
503 CONTROL_PLANE_UNAVAILABLE, AUTHORITY_SCHEMA_INVALID, DEPENDENCY_UNAVAILABLE,
LEASE_POLICY_UNAPPROVED. TRANSFER_PENDING includes Retry-After; effective_after
is returned only after authenticated explicit confirmation. Error details must
not disclose another device owner's identity to unauthenticated requests.

DPoP-Nonce response header bootstraps GET /session proof after challenge; the
challenge nonce is accepted directly in the signed proof. Body challenge_id on
POST must match that proof's nonce/purpose/key; body never supplies authority.
Challenges for retry/refresh/logout are available without current access token,
but completing a command still requires its credentials and bound proof.
Recover confirmation reuses a valid captured recovery_id and proof from the same
reauthenticated key, not an unbound boolean or caller tenant ID.

Schema is structural, not a substitute for semantic checks: trial start < end,
server_time and dates consistent with subscription/access_mode; active eligibility
cannot authorize past entitlement end even if authority incorrectly labels ACTIVE.
Lease NumericDate iat <= nbf < exp; exp never past authoritative end; context/lease
identities, epoch, revision and rights agree. A missing SR mapping is an explicit
activation data-repair gap, not a generated code. If control plane encodes expiry
as product inactive, require an authoritative reason distinguishing expiry from
suspension before allowing read-only login; never reinterpret a security denial.

Offline expiry preserves historical read access on the previously authenticated
bound device within its last validated role privileges. Lease exp ends write
rights; it never changes retained history into a write grant. New/unauthenticated
installations cannot use an expired lease to authenticate. Separate logout,
transfer and security-suspension history policy remains pending Owner decision.
