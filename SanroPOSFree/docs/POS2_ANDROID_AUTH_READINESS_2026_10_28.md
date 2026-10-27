# SANRO POS Public Android — POS2 Auth/Entitlement Integration Gate
**Date:** 2026-10-28 (Asia/Jakarta)  
**Status:** READ-ONLY SOURCE AUDIT / BLOCKED ON LIVE CONTRACT VERIFICATION  
**Android branch:** `feature/pos-free-functional-mvp-v1`  
**Android source base:** `7a518eaa588030505148379b6a623cffa3a5e16c` (UI test/hardware Owner UAT)

## What is verified from source (not deployment)
Reference: `arioguswara-oss/Sanro-POS-Stock`, branch `codex/sanro-public-release-dev-20261024`.

| Boundary | Source evidence | Android implication |
| --- | --- | --- |
| Browser login | `v2-login-register/runtime/web-server.js` accepts `POST /api/v2/auth/login` with same-origin/CSRF and sets Secure/HttpOnly/SameSite=Strict session cookie; `GET /api/session` reads session cookie | Do **not** assume this is a bearer-token endpoint. Do not extract cookies from WebView JavaScript, bypass CSRF or use credentialed cross-origin requests. |
| Separate bearer auth service | `v2-login-register/runtime/api-server.js` exposes `POST /api/v2/auth/login, /activity, /logout, /recover` using `http/login-handlers.js`. `runtime/README.md` calls it a separate candidate, not automatically installed on the public web host. | **NO evidence** that this service is exposed on `pos2.sanrotech.com`. Do not hardcode its routes as available in the APK. |
| Account/tenant gates | `lib/pos-login-service.js` validates verified email, ACTIVE account/product/tenant/subscription, and Free/Starter single operational session; `ACTIVE_SESSION_EXISTS` requires explicit recovery. | A second device must not silently take over. Offline operational lease requires separate approval and implementation. |
| Tenant identifiers | `runtime/TENANT_IDENTITY_CONTRACT.md`: internal `tenant_id` ST… is canonical storage identity; customer `tenant_code` SR… is independent and immutable. | Never generate ST from SR or accept either as client-selected authority. |
| Free operational data | `runtime/web-server.js` delegates `/api/pos/*` to cloud storage policy; Free local-first is outside DIRECT_CLOUD path. | Keep FREE sales/products/stock in tenant-bound Room/SQLite. No FREE writes to cloud operational POS endpoints. |
| Billing expiration rule | Owner rule: on trial/paid subscription expiry, POS becomes **read-only** with historical data still available. Older login source returns `SUBSCRIPTION_EXPIRED`. | Control plane + device must define read-only policy without inventing offline grace or treating expired as deleted. |

## Read-only live verification still required
The current runtime at `https://pos2.sanrotech.com` could not be probed from this ChatGPT environment (DNS unavailable), so no claim is made about deployed versions or endpoint responses.

On an authorized staging environment / read-only hosting inspection, determine:
1. Which server process is the actual `pos2.sanrotech.com` entrypoint; revision and release digest; whether private bearer API is exposed behind a mobile-specific proxy (do not publish it merely to pass a test).
2. Actual nonsecret GET health/CSRF/session envelope and session cookie policy; **never paste tokens/passwords/session cookies** into logs or issues.
3. Mobile login/activation path and HTTPS origin restrictions, with rate limiting, device association and safe logout/revocation.
4. Trustworthy entitlement response fields and server-issued validity: product/tenant/account ID, plan, status, effective/expires timestamps, revision, device binding, clock rollback controls and retryable errors. Do not trust local plan flags.
5. How Free initial online activation coexists with 1 active operational session; what to do after reset/restore, app reinstall or device replacement, and explicit owner-confirmed recovery.
6. Expired/offline handling: no write transactions after authoritative expiry; local history accessible (read-only) per Owner; no guessed grace period, no device-clock-only authorization. Separate paid plan modes (Starter shared DB, Professional+ cloud/hybrid) must not be accidentally activated in FREE app.
7. Live source may differ from GitHub branch; get read-only evidence before implementing API methods.
8. QA credentials must be dedicated nonproduction tenant; do not use Tunas Baru or live store records.

## Mobile auth contract to be approved (interface requirements, NOT invented routes)
### Request
- HTTPS, official verified base URL (no fallback to old `pos.sanrotech.com`).
- Credentials transmitted only to verified server endpoint; no credentials in code, logcat, URLs, GitHub, deep links or analytics.
- Each logical mobile operational installation holds a server-approved device/session identifier (no arbitrary client-selected tenant grant).
- PIN, password, OAuth, reauthentication and session recovery are distinct operations with their own permissions.

### Success response — minimum semantic requirements
- `authenticated=true` only after real backend verification.
- Authoritative `tenant_id` + `tenant_code` pair and `productAccountId`; server-bound `device/session` identity.
- `plan`, product/tenant/subscription statuses, authorized operations and effective expiration/revision.
- Secure short-lived authorization material, logout/recovery/revocation path and refresh mechanism.
- Signed or server-verifiable **offline entitlement/lease** only after an explicit policy is approved; current session TTL must not be silently treated as offline grace.

### Fail-closed outcomes
- Invalid/unverified account; incomplete registration; expired/inactive/read-only; suspension; Free ineligible; duplicate active session; invalid or replacement device; unavailable control plane.
- On uncertain auth response: no new operational sale, no new writes. Local readable historical data remains available to entitled previous device according to a separately approved retention/security policy. No data wipe.

## Safe Android implementation sequence
1. Isolate a `Pos2Auth` client module behind a mockable network interface and state-machine tests (no live login in CI, no hardcoded credentials).
2. Add native logged-out/login/verification/recovery UI. Until an approved mobile endpoint and device contract exist, these flows remain safely disabled or sandbox-only; never show false success.
3. Support secure token storage (Android Keystore-backed) after response contract approval; no `SharedPreferences` plaintext secrets.
4. Bind Room tenant partition from validated server authority; test cross-tenant partition denial and loss of device authorization.
5. Enforce server-originated read-only mode and offline lease without allowing clock rollback or multiple sessions.
6. Re-test Owner physical Bluetooth printer + auto cutter, offline restart/history, local sale idempotency, and no cloud FREE writes.
7. Preserve original proven debug app & printer pipeline; do not merge to main, sign release, AAB, or deploy before Owner UAT.

## Current result
**Native FREE cashier UI and Room local transactions:** built and tested in debug.  
**Panda PRJ-80BT print/cut/offline restart:** Owner UAT PASS on prior MVP; UI test safe-area/history display screenshot PASS.  
**POS2 native mobile login and live device/entitlement binding:** NOT IMPLEMENTED / CONTRACT GATE OPEN.  
**Production signing and Play Store:** NOT AUTHORIZED.

No hosting, database, Superadmin, POS2, printer, or application source changes were made by this audit document.
