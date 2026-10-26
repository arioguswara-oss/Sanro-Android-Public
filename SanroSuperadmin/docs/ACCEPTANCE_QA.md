# Android private V1.0.0 acceptance checklist

| Gate | Expected result | Status |
| --- | --- | --- |
| Build | Debug assembly succeeds on Java 17 / Android SDK 35 | PENDING |
| Install | App launcher/icon correct, no overwrite of POS/Stock | PENDING |
| Login | Existing production Superadmin email/password logs in | PENDING |
| Password update | Existing server password-change route succeeds | PENDING |
| Roles | User access enforced server-side | PENDING |
| Dashboard | Tenant table, navigation, Audit Performance visible | PENDING |
| Logout | Server session invalidated and Back cannot show protected content | PENDING |
| Expiry | Expired/revoked session returns to login | PENDING |
| Network | Offline state shows Retry; no stale private content | PENDING |
| TLS | Certificate failure cannot be bypassed | PENDING |
| Navigation | External / unsafe schemes rejected; only production Superadmin loads | PENDING |
| Mobile | Orientation, system navigation, keyboard, safe areas on target devices | PENDING |
| Privacy | No embedded secrets; backup disabled; screenshots in recents disabled Android 13+ | PENDING |
| Signing | Owner-owned release key securely backed up and signed release tested | PENDING |

**Release blocked until all applicable gates PASS.** Never record secrets/test passwords in the repository.
