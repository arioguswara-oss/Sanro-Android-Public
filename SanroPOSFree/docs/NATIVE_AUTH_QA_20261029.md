# Native auth implementation checkpoint — 2026-10-29

Branch: `feature/pos-public-android-native-auth-qa-20261029` (Issue #5).

## Implemented in this Android checkpoint

- Native email/password and device recovery screens. Submit clears the password
  and explicitly reports staging unavailable; it sends no credentials and grants
  no tenant/session.
- Non-exportable P-256 AndroidKeyStore installation key and AES-GCM refresh-token
  storage under `noBackupFilesDir`. Access tokens are not persisted.
- Strict tenant-context and lease semantic validators (identity/device/tenant,
  known statuses/plans/permissions, read-only expiry, same-boot deadline and
  24-hour maximum). Signature verification is an injected pinned-key interface.
- App Room calls moved behind `AuthorizedPosRepository`; each app read/write is
  checked at repository entry and sale-line reads verify the tenant-owned parent.
  The explicit debug-only sandbox remains isolated as `_DEBUG_LOCAL_ONLY_`.
- Added negative tests for locked repository access, unknown/mismatched context,
  lease signature/binding/expiry and fail-closed policy. Existing checkout,
  migration, printer and receipt tests were retained.
- Android debug workflow now runs on the requested feature branch and uploads the
  debug APK on successful build.

## Deliberately unavailable pending staging

`docs/mobile-auth-v1/openapi.json` names an `.invalid` placeholder and labels the
contract PROPOSED / NOT DEPLOYED. Backend staging has not supplied a routable HTTPS
origin, deployment revision, pinned lease verification keys, executable signed
fixtures, or a tested DPoP/JOSE dependency. Therefore this checkpoint has no HTTP
auth transport, refresh/logout/recovery exchange, signed-lease issuer, or grant
installation path. `TenantContextValidator` and `OfflineLeaseValidator` are
validation components only; synthetic schema examples cannot unlock Room.
Production login and tenant activation remain disabled. Reboot enforcement is
fail-closed for future grants via the boot identity comparison, but no trusted boot
anchor persistence/online revalidation client is wired yet.

## Verification status

- `git diff --check`: passed.
- `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`:
  not run; command unavailable (`gradle: command not found`), and Java is absent.
- No Android test, lint, APK, live server login, or physical printer result is
  claimed by this checkpoint. CI artifact is available only if GitHub Actions
  passes after push.
- No Room schema/data migration was made. Panda Bluetooth transport, ESC/POS
  receipt and cutter code were not changed; physical regression remains pending.
