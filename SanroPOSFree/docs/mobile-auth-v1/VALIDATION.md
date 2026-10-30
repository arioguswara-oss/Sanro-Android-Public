# Android native auth security implementation validation

Status: implementation branch, not staging acceptance. Backend source reviewed at
`arioguswara-oss/Sanro-POS-Stock` commit
`706fe2f5b413a9253ed4deded0870495c48e0746`; OpenAPI is `0.2.0-dev` with the
required `boot_id`, `require_online_after_reboot`, and
`lock_local_data_on_security_event` lease claims. Its origin remains deliberately
`.invalid`; no endpoint or Android trust root is approved.

Implemented on Android:
- Nimbus JOSE + JWT ES256 proof serialization, RFC 7638 public JWK thumbprints,
  AndroidKeyStore P-256 signing key and token `ath` binding.
- HTTPS-only DPoP target validation and strict, single-use challenge parsing. The
  challenge server time is advanced using elapsed realtime, not handset wall time.
- Compact JWS lease verification against explicit `kid` to public-key allowlists,
  with verification retirement deadlines, exact protected-header/claim sets,
  account/product/tenant/device/session/key/epoch binding, a 24-hour maximum,
  entitlement expiry, boot binding and mandatory reboot/security-event flags.
- Room policy lease installation only accepts a verifier-created one-use result.
  The debug sandbox is pinned to `DEBUG_SANDBOX`; normal policy cannot access it.
- Focused tests for signed lease validation failures, malformed challenges, tenant
  mismatch, reboot mismatch, epoch mismatch, permissions, expiry and sandbox lock.

Limitations and required gates:
- No verified Android signing trust root or rotation manifest is available. The
  verifier requires a configured trust set and therefore rejects all leases until
  that trust is provisioned through a reviewed release configuration.
- No approved HTTPS staging origin or genuine QA authority is available. No
  network login, refresh/logout/recovery, staging activation, persistent time
  anchor, or signed context-to-read-only grant path is enabled in the app.
- Local Gradle is unavailable in this workspace (`gradle` and wrapper absent).
  Android unit tests, lint, and debug build must be reported from GitHub Actions.
- Cryptographic unit-test leases are signed with an ephemeral test-only key and
  are not connected to Room or installed as operational grants.

No production/staging service or database was contacted. No Room schema/migration,
cashier transaction, history, printer implementation, or release-signing config was
changed. CI must run on this branch before these components can be considered
validated. Even a passing CI run does not establish staging readiness.
