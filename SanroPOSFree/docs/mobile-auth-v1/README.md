# Mobile auth integration handoff — WORK

Backend draft proposal: [Sanro-POS-Stock PR #16](https://github.com/arioguswara-oss/Sanro-POS-Stock/pull/16).
It contains documentation only and requires Owner review before server coding.
Tracking issues remain open: Android #4, backend #15.

- [Audit, design, fixed rules and remaining decisions](PROPOSAL.md)
- [Proposed OpenAPI and strict response schemas](openapi.json)
- [Staging acceptance specifications (not yet executed)](acceptance.feature)
- [Synthetic schema fixtures](schema-fixtures.json)
- [Pinned source evidence](source-evidence.json)
- [Performed validation and limits](VALIDATION.md)

Next: review proposal; select lease/revocation policy and authorize isolated
staging; inspect redacted deployed startup/revision and authority schema; approve
a separate adapter implementation PR; implement and pass staged acceptance;
then wire native Android auth/Keystore/Room gates and run hardware regression.
Do not unlock Android activation from this specification alone. Trial 3 months,
expiry Read-Only, single device and local Free storage are already fixed rules.
