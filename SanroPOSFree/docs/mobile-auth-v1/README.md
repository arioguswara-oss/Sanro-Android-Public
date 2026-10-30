# Mobile auth integration handoff — backend contract v0.2.0-dev

The OpenAPI and synthetic schema fixtures in this folder are synchronized to the
implemented, nonproduction backend adapter branch inspected on 30 October 2026.
They are not evidence of a deployed endpoint. Its server remains `.invalid` and
the backend source states the adapter is not deployed. Android network login and
trusted lease installation remain blocked pending an approved endpoint and key
trust configuration.

- [Audit, design, fixed rules and remaining decisions](PROPOSAL.md)
- [Proposed OpenAPI and strict response schemas](openapi.json)
- [Staging acceptance specifications (not yet executed)](acceptance.feature)
- [Synthetic schema fixtures](schema-fixtures.json)
- [Pinned source evidence](source-evidence.json)
- [Performed validation and limits](VALIDATION.md)

The Java Room gate and strict context/error policy remain fail-closed. Synthetic
fixtures cannot install a grant. Next: implement and review the Android DPoP
transport and pinned lease verifier against the backend contract, then run tests
through the existing Actions workflow. Keep network activation disabled until
staging endpoint, signing trust and genuine QA authority are separately supplied.
