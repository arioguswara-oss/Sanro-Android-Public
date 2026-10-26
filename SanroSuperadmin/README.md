# SANRO Superadmin Android — Private APK V1.0.2 (BASELINE LOCKED)

Android-only native WebView shell for the existing SANRO Superadmin server.

- Server: `https://sanrotech.com/superadmin/login`
- Package: `com.sanrotech.superadmin`
- Version: `1.0.2` (`versionCode=3`)
- Min SDK 26, compile/target SDK 35, Java 17, Gradle 8.9, AGP 8.7.3
- No Room, SQLite, local operational database, printers, Bluetooth, camera or embedded credentials.
- Debug builds have application ID `com.sanrotech.superadmin.debug` so they do not overwrite Owner-signed releases.
- Release APK is **NOT** signed by this repository. Create/backup Owner's private release keystore separately.
- GitHub Actions `workflow_dispatch` can build a debug/test APK; see `docs/BUILD_AND_RELEASE.md`.

## Branding
Launcher icon uses the approved SANRO Superadmin brand mark, cropped from the Owner-provided logo on 26 October 2026. Only the emblem is used at small launcher sizes; the provided design contains the full SANRO Superadmin wordmark. Icon is a proprietary SANRO brand asset.

## Password Autofill (V1.0.2)
The native WebView opts into Android Autofill, and the existing server HTML uses `autocomplete="username"` and `autocomplete="current-password"`. After a successful login page -> dashboard transition, the activity commits the Autofill context so the **user-selected** password manager can offer to save. Android / the provider controls whether an actual prompt or credential suggestion appears. The app never reads or persists the password natively, and no hardcoded credentials are shipped. Credential sharing with Chrome/website may additionally require Digital Asset Links and the final app's signing certificate; not included in this update. The server still owns authentication and its eight-hour session timeout.

## Security/operational principles

First-party HttpOnly server cookies remain the only session mechanism. WebView never reads session cookies or stores passwords natively. Only HTTPS pages at `sanrotech.com/superadmin...` can load as main frames. Third-party cookies, file/content access, cleartext, mixed content and WebView debugging are disabled. TLS certificate errors fail closed. Back navigation after login/logout clears stale login/session entries. Browser history is not a security boundary: server-side auth checks are mandatory on every protected route.

## Scope boundary

This standalone project is new. Do not edit existing `WORK_V1_0_5`, POS, Stock, server backend, database, signing assets or production. Source version in the Superadmin repository is not proof that the LIVE production runtime matches that commit. Owner approval remains required for production changes.

**Status (2026-10-26)**: Owner-verified Android V1.0.2 private signed release baseline LOCKED. GitHub contains source and unsigned CI candidates only; Owner holds the locally signed APK and release keystore separately. See [LOCK decision / UAT evidence / next roadmap](docs/BASELINE_LOCK_V1.0.2.md). Google Sign-In and Cloudflare protections are future discovery items, NOT part of the locked release.
