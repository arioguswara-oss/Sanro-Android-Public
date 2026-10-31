# Validation — proposal only

Status WORK, not staging acceptance. No backend implementation or Android runtime
changed in this contract task; no new APK or production verification is claimed.

Performed locally:
- JSON parse: OpenAPI, evidence manifest and fixture files.
- JSON Schema Draft 2020-12 meta validation: 23 component schemas.
- Reference resolution: 36 local `$ref` values resolve.
- Schema fixtures: 11 expected accept/reject results, including expired Free and
  paid read-only; malformed/missing binding, unknown plan and cloud mode rejected.
- `git diff --check`: clean.
- Existing backend source tests, Node 22.22.1, exit 0, 5/5 test files pass:
  `node --test test/pos-login-service.test.js test/session-activity.test.js test/pos-operational-access-gate.test.js test/access-context-client.test.js test/pos-free-activation-service.test.js`
  Run from the pinned snapshot's `v2-login-register/`. These use source doubles,
  not production or staged authenticated HTTP / MySQL race verification.

Not performed: full OpenAPI toolchain conformance lint, JOSE/DPoP interop, staging
HTTP calls, real MySQL concurrency, staged device/recovery, mobile token handling,
offline authorization, physical printer UAT. The 23 Scenario/Scenario Outline
sections in acceptance.feature are acceptance specifications, not an executable
passing adapter suite. They become step implementations only after backend coding
is approved. Structural JSON validation cannot certify signature or time logic.

Reproduce schema checks with Python 3 and installed jsonschema:

```python
import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker
p = Path('SanroPOSFree/docs/mobile-auth-v1')  # backend PR: v2-login-register/docs/mobile-auth-v1
api = json.loads((p / 'openapi.json').read_text())
for schema in api['components']['schemas'].values():
    Draft202012Validator.check_schema(schema)
for case in json.loads((p / 'schema-fixtures.json').read_text())['cases']:
    schema = dict(api['components']['schemas'][case['schema']], components=api['components'])
    valid = not list(Draft202012Validator(schema, format_checker=FormatChecker()).iter_errors(case['value']))
    assert valid == case['valid'], case['name']
```

The existing Android c705ed3 build had 14 passing tests, lint success with 9
warnings and a Debug APK in Actions 40566181169. That evidence is for locked
activation/sandbox behavior, **not** native login. No duplicate APK rebuild is
needed for this documentation-only proposal.
