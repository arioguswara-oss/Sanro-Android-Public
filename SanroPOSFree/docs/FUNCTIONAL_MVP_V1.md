# Local Free MVP — stage 1, Issue #2

Status: implementation candidate for Owner UAT, NOT production-ready. No POS2 activation, entitlement or commercial authorization is simulated.

## Audit before implementation

Android baseline: `93ea47275d3f37142fafe193d6bb9048e704126f`, branch `feature/pos-free-functional-mvp-v1`, clean at start. Baseline has four debug buttons, Room v1 products/sales/sale_lines, single-item atomic checkout, no request idempotency or migration. Android backup is disabled. Printer preference `printer_mac` belongs to the same MainActivity and debug application ID. Existing workflow only triggered pushes to the previous branch.

Read-only reference inspected: local checkout `/opt/sanro/projects/pos`, remote `arioguswara-oss/Sanro-POS-Stock`, HEAD `61e95928340bc5879759ec43ad96a971797117f3`. This is source evidence, not proof of the deployed POS2 runtime. Inspected `v2-login-register/lib/pos-login-service.js`, `pos-free-activation-service.js`, `pos-operational-access-gate.js`, and README. GitHub code search for CLOUD_PLAN_DENIED returned no matches; local source search did not locate this policy either. No network calls were made to POS2 LIVE.

| Contract | Source evidence / gap | Android decision |
|---|---|---|
| Tenant | Free activation validates `^ST\\d{5,}$`; Issue mentions SR00001 as sample | Do not assume sample code is an actual tenant ID |
| Registration | Verified email and registration/account/tenant binding required before Free activation | No fabricated registration UI or success state |
| Login | email/password; response includes outcome AUTHENTICATED_ACTIVE, session_token, expires_at, tenant_id, plan, single_operational_session, free_warning_active, free_days_remaining, free_expires_at | No token generation or placeholder login |
| Session | Opaque server token, default 12-hour TTL; FREE/STARTER one operational session; ACTIVE_SESSION_EXISTS; explicit recovery/revocation with credentials and confirmation for healthy session | Do not implement offline grace or automatic recovery |
| Entitlement | Server product ACTIVE, subscription ACTIVE; operational gate denies expired/inactive/suspended; FREE/STARTER/PRO in inspected source | Activation remains locked |
| Android device binding | No verified Android-specific device/token verification contract found | Needs approved endpoint/method, request device identity, response binding and authoritative validation |
| DIRECT_CLOUD / CLOUD_PLAN_DENIED | Exact deployed contract not established from inspected files | No `/api/pos/*` writes and no cloud bypass |
| Offline expiry | Session TTL does not define entitlement offline grace, clock rollback, restore or recovery policy | Requires Owner/control-plane decision |

Before activation integration: obtain approved nonproduction base URL + route/method mapping; registration and email-verification flow; authenticated tenant/account/device/session binding; authoritative plan/product/subscription/revision/expiry fields with validation semantics; error envelope/status/action for invalid, expired, suspended, duplicate session and CLOUD_PLAN_DENIED; revocation/recovery semantics; explicit offline expiry/grace/clock policy. These are required contract inputs, not proposed endpoints or invented token claims. Release does not select a debug tenant or open the operational DB.

## Implemented

- Native phone/tablet-width screens: cashier/search, products add/edit (SKU uniqueness), stock quantity adjustment, persistent cart, integer-rupiah cash tender and change, history/detail/reprint.
- Existing debug application ID/database/preferences retained, including printer MAC and prototype records.
- Explicit Room v1→v2 migration: persistent cart plus immutable sale-line product-name snapshots; old lines backfilled from products. No destructive fallback.
- Atomic multi-item checkout: validate product/tenant/quantity/stock/cash, debit all stock, save sale and snapshots, clear cart. Exceptions roll back all writes.
- Sale primary key is a durable checkout request ID. UI guards repeated payment submission; a retry returns the original sale without another stock debit/payment. ID is persisted before cart edits; committed checkout clears its cart atomically. Force close after commit leaves the sale in history and cart empty.
- Payment amounts are stored on the sale; no external payment gateway or network dependency.
- Reprints reload committed sale + immutable lines. Printing never writes transaction data. Original paired Bluetooth SPP transport is unchanged: 96-byte chunks, 45 ms pacing, 1200 ms final delay. Existing init/feed/full-cut command sequence retained, exactly one GS V 00 when cutter is enabled. Printer selection key unchanged. Send status is not physical acknowledgement; never auto-retry uncertain output.
- Release activation screen fails closed before operational database selection. Debug explicitly labels sandbox mode.

## Verification and remaining scope

CI runs Room/Robolectric unit tests (atomic cash/multi-item, stock and tender rollback, concurrent idempotency, tenant isolation, reopen persistence/snapshots, v1 migration), ESC/POS byte tests, Android lint and debug assemble. Test/lint reports and debug APK are uploaded by the branch workflow. Local host has no Java/Gradle/Android SDK; record actual CI outcome before claiming PASS.

Hardware PASS applies only to Owner-tested baseline: install/open, Panda PRJ-80BT Bluetooth, cutter, offline printing and reprint after force close. New APK requires repeat physical UAT; automated byte tests cannot establish no hardware regression.

Deferred: verified POS2 login/registration/device binding/entitlement, approved expiry/grace, cash-close and physical cash count rules, append-only inventory-adjustment audit/stock movement UI, separate payment ledger, returns/purchases, backup/restore/export, pagination for large catalog/history, retained payment progress UI during Activity recreation. Current stock edits replace available quantity; price/stock are revalidated at checkout. Cart shows current catalog prices. No release signing, keystore or AAB is created.

## Owner UAT checklist

1. Upgrade baseline debug APK without uninstall: old history, product, printer selection remain. Uninstall/clear-data deletes local data because backup is disabled.
2. Add two distinct SKUs; reject duplicate SKU and invalid/negative price/stock. Edit names/prices/stock. Search by name/SKU.
3. Build multi-item cart; force close/reopen offline: cart and products remain. Confirm quantities and cash change.
4. Reject insufficient cash/stock, verify all stock and history unchanged. Rapidly tap payment: exactly one sale and one stock debit.
5. Complete payment; force close/reopen offline: committed sale in history, empty cart, updated stock. Change product name/price: old nota remains unchanged.
6. Select paired Panda PRJ-80BT; print a multi-item saved sale including long names/footer; verify full content, one cut, no extra payment/debit. Disconnect/reconnect, force close/reopen and repeat offline reprint.
7. Inspect release activation gate through a future unsigned QA build if authorized; it must not expose debug operations. This phase builds debug only.

Rollback reference: `93ea47275d3f37142fafe193d6bb9048e704126f` remains untouched. Do not install old v1 app over migrated v2 data: Room downgrade is intentionally unsupported. Use a separate test device/profile for old prototype comparison; do not clear data to roll back without explicit Owner consent.
