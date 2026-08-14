---
schema_version: 1
open_count: 1
waived_count: 0
fixed_count: 0
total_count: 1
last_updated: 2026-08-14T22:26:03.998Z
---

# Broken Windows Ledger

> Cross-phase defect register. With `workflow.windows_enforce` enabled, `/gsd-ship` blocks while `open_count > 0`.
> Waive with `gsd-tools windows waive <id> "<reason>"` (reason required).
> Mark fixed with `gsd-tools windows fixed <id>`.

| id | phase | kind | file | line | description | status | reason | recorded_at | resolved_at |
|----|-------|------|------|------|-------------|--------|--------|-------------|-------------|
| 1 | 03 | deviation | app/src/main/java/org/dergigi/boris/nostr/SignerResult.kt |  | parseSignedEvent event overload because org.json is stubbed on JVM | open |  | 2026-08-14T22:26:03.998Z |  |

````json
[
  {
    "id": 1,
    "kind": "deviation",
    "phase": "03",
    "file": "app/src/main/java/org/dergigi/boris/nostr/SignerResult.kt",
    "line": null,
    "description": "parseSignedEvent event overload because org.json is stubbed on JVM",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-08-14T22:26:03.998Z",
    "resolved_at": null
  }
]
````
