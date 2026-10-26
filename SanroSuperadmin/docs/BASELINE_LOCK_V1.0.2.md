# OWNER-APPROVED LOCK — SANRO Superadmin Android Private V1.0.2

**Decision date:** 2026-10-26 (Asia/Jakarta)
**Status:** BASELINE LOCKED — OWNER APPROVED
**Scope:** Private Android APK release only. This does not freeze or change the live Superadmin server, database, POS, Stock, or other products.
**Authoritative source snapshot:** `bfc657f6c96fbf5c3a56db09162bf5b2b0f09ba0` (before this documentation-only LOCK commit).
**Private signed release artifact:** Owner-generated locally, NOT stored in public GitHub.
**Distribution:** Private and authorized users only; no Play Store publication approved.

## Frozen release identity
- Application ID: `com.sanrotech.superadmin`
- Version name: `1.0.2`
- Version code: `3`
- Runtime: native Android shell + existing server-hosted `https://sanrotech.com/superadmin/login`
- Launcher: Owner-supplied SANRO Superadmin brand mark
- Credential experience: Android/Google Password Manager Autofill, existing server account authentication
- No SQLite/Room/offline transaction engine; existing server remains authoritative
- Private signing: Owner-owned `sanro-superadmin-release.jks`, alias `sanro_superadmin`; kept and backed up independently; NEVER committed/uploaded
- Signer certificate SHA-256: `7A598EEBE133D78FC011D9BEB9E51187591AAFBFC5D27CA3C4C66DB849245548` (public certificate identifier only, NOT key or password)

## Owner-verified acceptance, 2026-10-26
- Release APK installs and opens on Owner's Android device.
- Existing Superadmin email/password login succeeds; dashboard and tenant Tunas Baru accessible.
- Google Password Manager autofills saved credentials.
- Back from tenant to dashboard succeeds.
- Logout -> Android Back -> reopen shows login instead of protected dashboard.
- Locally executed ZIP align, release signing, apksigner verify (v2/v3), and signer SHA-256 matched the Owner's keystore certificate.
- GitHub Actions unsigned release CI passed test/lint/assemble/identity/unsigned verification.
These observations support LOCK of the **tested private functional baseline**. They are not a blanket security audit, proof of cross-device compatibility, or proof that every auth/network edge case was tested.

## Freeze and release discipline
- Do not change V1.0.2 code, business behavior, app identity, signing assets, or server behavior as part of this LOCK.
- All further feature/security changes require separate Owner-approved scope, UAT, and a new Android version code (minimum 4) when shipping updated APK.
- Use the SAME Owner-controlled signing key for all installable upgrades of `com.sanrotech.superadmin`.
- Preserve the locally signed final APK, verified certificate fingerprint, and independently backed-up keystore; repository CI artifacts are intentionally unsigned.
- New development must not edit POS, Stock, or live production infrastructure incidentally.
- Open further QA: forced session expiry/revocation, offline/retry, TLS failure, other Android versions/devices, password-change, role permissions and any newly introduced login/security flows.

## NEXT ROADMAP — NOT YET IMPLEMENTED / NOT PART OF V1.0.2
### A. Sign in with Google
- Owner-requested idea for a subsequent phase; discovery and Owner approval required before implementation.
- Authorized invited users only; no anonymous self-registration. Account matching, permissions, revocation, and audit remain server-side.
- The current Android shell embeds a WebView. Google OAuth must **not** run inside embedded WebView; plan a compliant Android Credential Manager/native Sign in with Google or external browser/Custom Tabs flow with secure, tested handoff into server session.
- Keep existing email/password login as fallback until a replacement is approved and proven.
- Design must include the Superadmin web/backend and Android paths; no production change currently authorized.

### B. Cloudflare security
- Owner-requested future defense layer for the Superadmin hostname/path; discovery and Owner approval required.
- Evaluate WAF managed rules, login POST rate limiting, bot controls, TLS/origin protection, monitoring/logging, and exception recovery.
- Preserve real users' access through Android WebView, ordinary browsers, and future Google auth; challenge/captcha rules must be tested carefully to avoid lockout.
- No Cloudflare DNS, proxy, zone, WAF or firewall changes authorized by this document.

**Decision boundary:** V1.0.2 LOCK applies to the current proven Android release only; Google Login and Cloudflare are backlog items, not approved implementation design.
