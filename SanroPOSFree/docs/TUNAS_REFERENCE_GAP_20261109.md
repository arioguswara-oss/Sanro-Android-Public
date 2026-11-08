# SANRO POS Public Android — Reference-Based Development Gap Map

**Checkpoint:** 9 November 2026 (Asia/Jakarta)  
**State:** DEVELOPMENT REFERENCE / NOT RELEASE READY  
**Priority:** P1 — Android Public Release Candidate

## Authority, privacy and isolation

- **Source of truth for Tunas Baru operational behavior:** owner-verified **LIVE hosting** `pos.sanrotech.com`, only when its exact live version and scope have been read-only verified; a GitHub archive describing LIVE is **secondary evidence**, not a fresh hosting runtime audit.
- **Latest reference available in GitHub at this checkpoint:** private `arioguswara-oss/Sanro-Pos-TunasBaru`, `main` at `e6e856865e68bfcdd2db4e5c73c93916a57be482` (archive committed 8 Nov WIB); baseline document includes Nov 3–8 owner-verified LOCK checkpoints. See source repo `TUNASBARU_BASELINE.md`. This is a reference checkpoint, **not a claim that all GitHub code equals deployed LIVE**.
- **Public Android code inspected:** this repo `SanroPOSFree/`, feature branch `feature/native-auth-security-implementation-20261030`, pinned at `3724147d42389189efb1fa2de4489a4da8892552` (Draft PR #7); cash/Room and print development is tracked in Draft PR #3 and authentication in #6/#7.
- **Tunas Baru is read-only.** No production commands, mutations, migrations, extracts, restart, CI against production, copying hosted files, private source bundles, credentials, customer data or live SQL into Public or issue/PR comments. This public document contains only behavioral requirements and nonsecret commit identifiers; never copy private-source snippets or internal transaction samples.
- Tunas Baru LOCK modules must not be changed or regression-tested by Hermes. Financial work OPEN on Tunas Baru is not permission for Public to touch it. Cash Drawer is explicitly postponed, not a Public release requirement.
- Public app is a **separate product**: `com.sanrotech.pospublic` (debug variant distinct), account/tenant billing, plan-specific data storage, tenant isolation, Android entitlement, subscription expiration; production Tunas Baru `com.sanrotech.pos` is not its backend.
- Reference behaviors are not automatic change requests. Owner's latest product decision and independently verified Public code govern what gets implemented.

## Verified evidence vs remaining acceptance gaps

Key: **CODE TESTED** = code and recorded CI exists at exact branch SHA; **OWNER HARDWARE BASELINE** = prior limited physical result; **PARTIAL** = implementation missing end-to-end integration; **NOT VERIFIED** = no acceptable evidence of this Public behavior, not necessarily no code.

| Public feature / reference behavior | Public Android evidence at checkpoint | Missing gate and priority |
| --- | --- | --- |
| FREE local-first cashier, catalog, persistent cart, multi-line cash sale, atomic stock decrement, idempotent checkout and immutable reprint | **CODE TESTED** — Draft PR #3 contains Room v1→v2 migration, automated checkout/stock/tenant tests, cash change, receipt rendering; its CI reported unit/lint/assemble PASS. PR #7's source tree retains the Room/print module; **exact combined branch must re-run checks**. | **P0:** no trusted real-account activation yet; physical combined-build UAT still needed. |
| Sale history / quick reopen / saved print, avoidance of duplicate payments | **PARTIAL** — immutable saved sales and idempotency tests exist in PR #3. Newer Tunas Baru archive records owner-accepted optimized sales history/print; not automatically transferable. | **P1:** large-data pagination, real-device performance, restart and receipt rendering regression; no claim of equivalent speeds. |
| Panda PRJ-80BT Bluetooth receipt, ESC/POS cut, reconnect, offline reprint | **OWNER HARDWARE BASELINE** — Public prototype was physically tested and reported working (Public Issue #2/#5). PR #3 reports printer byte tests and debug APK, PR #7 preserves printer code. | **P0:** exact final combined APK must pass Owner physical print/cutter, offline, app restart, long receipt, no duplicate or cash/stock side effect. |
| Dual-cashier shared Bluetooth printer, print one at a time (FIFO), no duplicate, no forced pairing disconnect | **NOT VERIFIED** in released Public candidate. | **P1:** enforce approved queue behavior per eligible Cloud multi-cashier architecture; physical two-device acceptance separate from first FREE RC. |
| Registration, verified email login, tenant/device binding, role/entitlement, free-trial, subscription READ_ONLY | **PARTIAL** — Draft PR #6/#7 adds fail-closed native login screens, device crypto/lease, gated Room; CI unit/lint/debug build previously PASS, but no real HTTPS authority. | **P0 BLOCKED:** authentic isolated staging account/role/tenant/billing authority, approved HTTPS origin, trust/signature lifecycle and exact backend contracts; no fabricated session, token or activation. |
| FREE/Starter local operation; Professional/Business Cloud; migration and offline safety | **PARTIAL** — FREE Room exists; no end-to-end verified hybrid/Cloud plan handoff. | **P0/P1:** reconcile final plan decision, implement only approved policy. GitHub Public Issue #1 contains an older conflicting assertion about Starter storage; do not silently adopt it over later Owner decisions. |
| Master Barang and contact data (barcodes, no-barcode scan, 2 photos, multiline text) | **PARTIAL** — basic local SKU/price/stock in PR #3. Tunas Baru reference has a richer approved workflow. | **P1:** define scoped Public fields + barcode/camera acceptance; no image or contact private data copy. |
| Stock mutation, stock opname, bulk progress, no negative quantity | **PARTIAL** — PR #3 local stock adjustment/check-out validations; **no evidence** of full Stock Opname workflow. Recent Tunas Baru archive refers to approved Bulk V2 and responsive UI. | **P1:** stock movement/audit entries, opname count reconciliation, negative-input rejection, progress, per-tenant consistency and regression. |
| Per-item/invoice discount, quantity decimal, bundle items; CASH/TEMPO; cancellation lock | **NOT VERIFIED** beyond basic cash MVP; Tunas Baru has owner-accepted richer sale rules. | **P1:** review Public semantics and implement on local/cloud modes, preserve atomic stock/HPP audit and no duplicate sale. |
| Supplier purchasing, returns, payables/receivables and settlement | **NOT VERIFIED** in current Android FREE scope; Tunas Baru has behavior references only. | **P1/P2:** implement by Public entitlement/plan, reversible audit and proper inventory and finance boundaries. |
| Manual daily cash close, grouped store/cashier, adaptive physical cash, CASH/QRIS/BANK | **NOT VERIFIED** — Public PR #3 defers close and distinct payment ledger. | **P1:** Owner's approved adaptive cash-close per package with role gates, cash/transfer/QRIS breakout, receivable settlement separate from turnover. |
| Custom PKP/Non-PKP, multi-owner, Point Reward, Cash Drawer | **EXCLUDED / NOT REQUIRED** for simplified Public where removed or explicitly postponed; global PPN Off/On remains a separate Public decision. | **Do not port.** |
| Distribution-ready Android build, release signing, Play closed-test acceptance | **NOT VERIFIED** — existing GitHub Actions artifacts are **debug prototypes** with expiration dates, not a release APK/AAB. | **P0 EXIT:** clean build with exact manifest/SHA256, real device + printer + tenant staging UAT, documented rollback, Owner reviews and performs release/Play actions. |

## Owner-approved implementation order — project backlog, not dispatch authority

1. **P0-1: source lineage and combined build.** Pin and compare PR #3/#6/#7 ancestry; avoid losing the tested Room/print implementation. Produce an exact combined commit, build/test evidence and new nonproduction debug artifact, with regressions. Do not merge to an auto-deploy branch without review.
2. **P0-2: isolated real mobile auth + entitlement.** Resolve backend staging authority / signers / exact native routes, demonstrate login → tenant device bind → approved operational mode → logout/read-only/transfer/recovery and offline lease. Must remain fail-closed pending genuine authority.
3. **P0-3: hardware acceptance for Public Android.** Panda PRJ-80BT physical UAT after authentication; ensure print never changes sale/payment/stock, and no double-print after retry. No Tunas Baru production access.
4. **P1-1: business-function parity on Public terms.** Add accepted sale/discount/decimal/tempo/cancel, stock movement/opname and cash close within plan scope; build tests against synthetic tenant data; separate advanced supplier/returns/finance into traceable tasks.
5. **P1-2: multi-cashier and Cloud.** Test simultaneous role/device limitations, sequential printer FIFO for supported packages, subscription expiry READ_ONLY, Starter→Professional migration and cloud boundaries.
6. **Release Candidate:** pin source and reproducible APK, record SHA256, Android/device matrix and failures, user-facing release notes and rollback. **Stop at READY FOR OWNER**; Rio alone authorizes LIVE deployment, extract/installation and Play publication.

## Hermes handoff and limitations

- **Do not pretend this document is an executable worker job.** The currently activated `hermes-pos-continuous-worker` accepts only expiring structured Owner-authored tasks for one `v2-login-register/lib/*.js` file per execution. It **does not** read Android Issues or build this repository.
- A separate isolated **Android project planner + worker** with scoped GitHub identity, independent worktrees, Android SDK/build toolchain, bounded retries, protected secrets, resource limits and explicit stage/dev authority must be implemented and runtime-verified before Hermes can autonomously consume this backlog.
- For new Tunas Baru update: read-only inspect **changed functional behavior** + verified owner acceptance at exact current host revision where permitted; update only this gap report with evidence; do not copy source or infer that an archive alone is runtime LIVE.
- Progress proof must use exact source SHA/CI/device result. `IMPLEMENTED` ≠ `TESTED` ≠ `STAGED` ≠ `LIVE`. Never claim APK final from a Draft PR.

**Current checkpoint:** reference mapping `IMPLEMENTED`; Public Android release readiness **NOT VERIFIED**, current LIVE Tunas Baru parity **NOT VERIFIED via direct hosting inspection**.
