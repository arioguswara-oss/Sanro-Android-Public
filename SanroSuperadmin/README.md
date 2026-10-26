# SANRO Superadmin Android — Private APK V1.0.0 candidate

Android-only native WebView shell for the existing SANRO Superadmin server.

- Server: `https://sanrotech.com/superadmin/login`
- Package: `com.sanrotech.superadmin`
- Version: `1.0.0` (`versionCode=1`)
- Min SDK 26, compile/target SDK 35, Java 17, Gradle 8.9, AGP 8.7.3
- No Room, SQLite, local operational database, printers, Bluetooth, camera or embedded credentials.
- Debug builds have application ID `com.sanrotech.superadmin.debug` so they do not overwrite Owner-signed releases.
- Release APK is **NOT** signed by this repository. Create/backup Owner's private release keystore separately.
- GitHub Actions `workflow_dispatch` can build a debug/test APK; see `docs/BUILD_AND_RELEASE.md`.

## Security/operational principles

First-party HttpOnly server cookies remain the only session mechanism. WebView never reads session cookies or stores passwords natively. Only HTTPS pages at `sanrotech.com/superadmin...` can load as main frames. Third-party cookies, file/content access, cleartext, mixed content and WebView debugging are disabled. TLS certificate errors fail closed. Back navigation after login/logout clears stale login/session entries. Browser history is not a security boundary: server-side auth checks are mandatory on every protected route.

## Scope boundary

This standalone project is new. Do not edit existing `WORK_V1_0_5`, POS, Stock, server backend, database, signing assets or production. Source version in the Superadmin repository is not proof that the LIVE production runtime matches that commit. Owner approval remains required for production changes.

**Status**: code candidate / not device tested / no production deployment / no signed release.
