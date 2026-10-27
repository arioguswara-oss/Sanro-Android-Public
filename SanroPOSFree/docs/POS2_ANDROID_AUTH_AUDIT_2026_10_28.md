# POS2 Android login / tenant activation audit

Status: **BLOCKED for real native login**. Issue #4. Development branch:
`feature/pos-free-functional-mvp-v1`. No backend, hosting, production database,
Superadmin or Tunas Baru changes. No authenticated production probes.

## Evidence and limits

Readiness document `POS2_ANDROID_AUTH_READINESS_2026_10_28.md` and GitHub Issue #4
were read. That document uses 28 October WIB; this verification ran on
27 October 2026 at approximately 19:02–19:05 UTC (28 October WIB).
HTTPS certificate verification remained enabled. Sandbox DNS failed; authorized
read-only HTTPS outside the sandbox succeeded. No POST, login, recovery,
customer credential, session mutation or operational data request was performed.
CSRF GET issued an anonymous cookie; its value was discarded and never retained.

| Live GET at https://pos2.sanrotech.com | Result | What this proves |
| --- | --- | --- |
| `/` | 200 HTML | Public website reachable |
| `/api/runtime-info` | 200 `{"ok":true,"data":{"demo":false}}` | Server reports non-demo; not readiness or entitlement |
| `/health/live` | 200 `{"ok":true,"scope":"process"}` | Process responds, not DB/mail/authority readiness |
| `/api/session` without cookies | 401 `{"ok":false,"error":{"code":"AUTH_INVALID"}}` | Anonymous request denied; success envelope unverified |
| `/api/csrf` | 200 `{ok:true,csrf:<redacted>}` | Cookie attributes Path=/; Secure; SameSite=Strict; Max-Age=3600; HttpOnly; `Cache-Control: no-store` |
| `/api/v2/auth/login` | 404 `NOT_FOUND` | GET result only; **does not establish POST absence** |
| `/api/v2/auth/activity` | 404 `NOT_FOUND` | GET result only; **does not establish bearer API presence/absence** |
| `/assets/app.js` | 200 JavaScript | Browser client uses CSRF and same-origin requests |

Live app.js SHA256:
`68504fed635e43bdbc39aa1297659ec5e1569d86e66bb5cf9ca9cb3c48b8f3a2`.
It references POST login (email/password), recover (email/password and
`confirm_revoke:true`), logout, verification and password reset. These are
**client references**, not verified POST contracts. No bearer or refresh contract
was established. Hosting startup file, revision and release digest remain
unverified: public responses do not reveal them. No hosting connector inspection
was performed. Owner/backend operator must supply a redacted read-only deployment
manifest or authorize an available read-only inspection channel.

Source inspected via GitHub, reference branch
`codex/sanro-public-release-dev-20261024`:

- `v2-login-register/runtime/README.md`, blob `129543dca54676cc2612867801a17583e9733ae8`:
  separate bearer API candidate; web launcher excludes that private entrypoint.
- `runtime/web-server.js`, blob `ea44523e2b1dba61f9c497695d825e90e4ba358e`:
  same-origin CSRF, Secure/HttpOnly/Strict session cookie; removes `session_token`
  from login JSON; protected requests reauthorize. Browser cookie extraction is
  unsuitable for native login.
- `runtime/api-server.js`, blob `ce6cbd2318f64bb9e6a9d75d9376174eac46f643`:
  separate POST bearer service, not proof of deployment.
- `http/login-handlers.js`, blob `975e09b70622619e0c424e865dd2b7137e8915b3`:
  `{ok:true,data,request_id}` / `{ok:false,error:{code,message,retryable,action},request_id}`;
  login email/password, activity/logout Bearer, recovery explicit confirmation.
  No device binding is passed by these handlers.

Source paths above are relative to `v2-login-register/` except README. Live
health response and operator references show why source cannot certify deployed
revision. Existing readiness findings on ACTIVE gates, single session and ST/SR
are source evidence, not newly verified authenticated live behavior.

## Android audit and implemented boundary

Existing app has no HTTP authentication client, token store, cloud POS transport
or authenticated tenant. Room v2 uses `sanro-pos-public-local-v1`, tenant columns,
atomic checkout/idempotent request IDs. Manifest disables backup and cleartext.
Before this change debug entered `_DEBUG_LOCAL_ONLY_` automatically; release
already returned before selecting a database. This change starts at a locked
native activation screen in every build. Login/recovery are disabled with honest
status. Debug requires explicit sandbox entry and uses the same application ID,
database name, tenant, printer preference and receipt pipeline. Restart requires
sandbox entry again; stored receipts are retained. No schema migration/data wipe.
No production tenant can be selected, granted or restored by these controls.

Sandbox is intentionally a test-only exception, **not authenticated operation**.
Release has no sandbox button and its entry method checks DEBUG. Direct Room DAO
methods are not yet an authenticated repository boundary; they must not be
connected to real tenants until the binding/gate below exists. SaleLine reads
are keyed by sale ID, so future authenticated repositories must first prove
parent sale ownership (including all read/export/reprint paths).

## Native mechanism and backend gaps (requirements, not deployed API)

Prefer a dedicated native short-lived opaque bearer session backed by server
revocation and rotating refresh credentials. Reuse audited backend services
behind a reviewed mobile adapter; never expose a private service solely to make
Android pass. An authorization-code + PKCE external browser flow is an alternative
only if backend provides issuer, metadata, registered redirect and code exchange.
There is no evidence of OAuth today. Do not embed a service key, fake browser
Origin/CSRF, extract WebView cookies, or map browser session TTL to offline rights.
No mobile route or DTO is invented in the APK.

Backend must supply and prove in an isolated staging tenant:

1. Official HTTPS origin, exact methods/paths, request/response/error schemas,
   versions, body limits, rate limits and redirect policy. Verified login success
   and malformed/error fixtures. Credentials sent only in TLS request bodies.
2. Authoritative account/productAccountId, canonical internal tenant_id (ST) and
   immutable display tenant_code (SR). No SR-to-ST derivation or caller-granted
   tenant/plan. Email verified; account/product/tenant/subscription gates explicit.
3. Server-bound device public-key identity and session ID, plan FREE, permissions,
   effective/expiry/server time, revision, storage mode LOCAL_ONLY. All required
   fields validated; unknown plan/status/mode or tenant/device mismatch denies.
4. Atomic one-device activation across concurrent logins, activities and refresh;
   duplicate active session returns conflict. Never silently recover. Recovery
   requires explicit Owner confirmation/reauthentication, revokes old device and
   token family, then separately activates the new device. Retries idempotent.
5. Token lifetime, rotation/replay detection, expiry, refresh, revoke/logout and
   lost-response behavior. Timeout/5xx/invalid JSON cannot unlock operations.
6. Approved signed offline lease format, issuer/key rotation, audience/device/
   tenant/session/revision/permissions and absolute validity; revocation latency
   and reconnect requirements. No arbitrary grace. Server must explain how
   one-device operation coexists with an old offline device after recovery.
7. FREE trial duration and authoritative start/end versus permanent Free policy;
   expiry must disable writes without deleting history. Approve historical read
   access/reprint after expiry, suspension, logout, lost device and tenant switch.
   No use of prior stored tenant history by a newly unauthenticated installation.

## Android implementation plan after contract approval

- Mockable native auth transport, strict bounded parser, TLS only, no redirects
  across origins, no secret logging. Login state progresses locked -> pending ->
  authenticated but unentitled -> active only after verified binding/rights.
  Duplicate session shows explicit recovery choice; cancellation stays locked.
- Separate authorization policy/repository from UI: every product/cart/stock/sale
  write checks current grant inside the serialized transaction boundary. Check
  again when queued work runs, including checkout. Unknown/revoked/unavailable
  state denies writes. Expiry yields read-only only under approved history policy.
- Local Room stays authoritative for FREE operational data. Control-plane auth,
  device and entitlement requests contain no sales/product/stock payloads. No
  `/api/pos/*` transport for FREE, no direct Cloud DB driver/credentials. Existing
  sandbox data never relabelled or promoted into an activated tenant.
- Bind Room access to validated account+ST+device identity; resolve tenant through
  the authorization repository, not an intent/preference/input. Partition and
  verify parent-child reads. Preserve database/migrations and checkout identity.
- Keep access token in memory. Encrypt rotating refresh token with AES-256-GCM
  using a nonexportable AndroidKeyStore key, fresh random IV per write and AAD
  binding version/account/tenant/device. Atomic ciphertext in private
  `noBackupFilesDir`; never plaintext preferences. Separate installation signing
  key for server-approved device binding; no hardware ID as authority. Keystore
  errors/invalidation/tampered ciphertext clear authorization and require online
  recovery; keep operational database. Password discarded after request.
- No token backup/restore. Reinstall, restore, device replacement and logout do
  not auto-grant rights. Logout clears local authorization immediately and queues
  safe server revocation semantics as contract allows; offline logout never
  claims remote revocation succeeded. A stored token alone never opens the till.
- Offline lease verification needs trusted server time anchored to monotonic
  elapsedRealtime within boot, persisted anti-rollback state and online recheck
  after reboot unless a stronger approved mechanism exists. Wall-clock changes
  cannot extend rights. No offline lease is implemented or granted today.

## Tests and Owner decisions

Added automated launch/recreation test: db and tenant remain null; login/recovery
are disabled; no checkout button. Existing Room/receipt suites remain required.
CI runs testDebugUnitTest, lintDebug and assembleDebug only. Local machine lacks
Java/Gradle/SDK; GitHub Actions is the build verification environment.

Required after backend approval (not claimed passing today): invalid/unverified
account, expired/suspended/read-only subscription, missing/unknown/malformed fields,
HTML/redirect/timeout/5xx, duplicate session and concurrent recovery, token refresh
race/replay, tenant/device mismatch, revoke during queued checkout, clock rollback,
reboot, key invalidation, reinstall/restore, offline lease expiry, history isolation,
zero FREE Cloud POS writes and no secrets in APK/logs/backup. Use dedicated
nonproduction fixtures only. Owner must physically retest Panda PRJ-80BT, cutter,
reprint after force close, safe area, checkout retry and preserved local history
on this exact new APK; previous hardware UAT does not certify this build.

Owner decisions blocking real login: approved mobile mechanism and deployment
contract; isolated QA account; Free/trial entitlement policy; offline lease and
one-device recovery/revocation tradeoff; historical access policy. Backend work
and its production rollout require a separate approved task. No merge/deploy,
release signing or AAB is authorized here.
