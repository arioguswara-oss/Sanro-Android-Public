# Mobile auth integration handoff — backend contract v0.2.0-dev

The OpenAPI and synthetic schema fixtures in this folder are synchronized to the
implemented, nonproduction backend adapter branch `codex/mobile-auth-qa-fixes-20261030`
at `706fe2f5b413a9253ed4deded0870495c48e0746`, inspected on 30 October 2026.
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

The Android branch contains a non-exportable AndroidKeyStore ES256 DPoP proof
builder, strict single-use challenge parsing, and an allowlisted-key lease verifier
with monotonic expiry and boot/tenant/device/account/epoch binding. Nimbus JOSE +
JWT handles JOSE serialization, JWK thumbprints, signature verification and the
standard ECDSA JWS encoding. Room grant installation accepts only verifier-created
leases. These components do not activate login or provide a deployed transport.

Trusted signing keys and an approved HTTPS origin have not been supplied. Signed
test tokens use an ephemeral unit-test key and are never installed into app state.
Android network login and real lease installation remain disabled pending verified
staging endpoint, trust-root/rotation metadata and genuine QA authority.
