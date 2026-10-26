# Android private V1.0.2 acceptance checklist

| Gate | Expected result | Status |
| --- | --- | --- |
| Build | CI tests, lint, release assemble and package verification succeed | PASS (CI) |
| Install | Owner-signed private APK installs and opens, expected branded launcher | PASS (Owner device) |
| Login | Existing production Superadmin email/password logs in | PASS (Owner device) |
| Password update | Existing server password-change route succeeds | PENDING |
| Roles | User access enforced server-side | PENDING |
| Dashboard | Dashboard and tenant Tunas Baru visible, Back returns dashboard (other pages not fully checked) | PARTIAL PASS (Owner device) |
| Logout | After Logout -> Back -> reopen, protected dashboard not available without login (server-side revocation not independently probed) | PARTIAL PASS (Owner device) |
| Expiry | Expired/revoked session returns to login | PENDING |
| Network | Offline state shows Retry; no stale private content | PENDING |
| TLS | Certificate failure cannot be bypassed | PENDING |
| Navigation | External / unsafe schemes rejected; only production Superadmin loads | PENDING |
| Mobile | Orientation, system navigation, keyboard, safe areas on target devices | PENDING |
| Privacy | No embedded secrets; backup disabled; screenshots in recents disabled Android 13+ | PENDING |
| Password Autofill enabled | Google Password Manager suggestions and automatic fill of email/password | PASS (Owner device) |
| Password Autofill disabled | Login works by manual entry with no new errors | PENDING DEVICE QA |
| Credential security | Saved credentials remain in user-selected manager, not local SANRO app database | PENDING DEVICE QA |
| Signing | Owner-owned release key backed up; signed APK verified v2/v3 and fingerprint match | PASS (Owner local CMD) |

**Baseline V1.0.2 is Owner-LOCKED for tested private functionality only; open entries remain future hardening/UAT gates, not claims of complete security certification.** Never record secrets/test passwords in the repository. Refer to [LOCK record](BASELINE_LOCK_V1.0.2.md).
