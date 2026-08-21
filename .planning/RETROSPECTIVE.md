# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.5 — Reader

**Shipped:** 2026-08-21
**Phases:** 6 | **Plans:** 11

### What Was Built

- Amber and bunker identity without an nsec
- NIP-84 highlights in the reader
- On-device TTS with follow-along and a mini player
- nprofile mentions as profile links
- On-device article extract; Jina path removed

### What Worked

- Shipping 1.4.x patches while phases were still open let daily use find ANRs and extract lag
- Closing UAT from dogfooding matched how phase 3 was already accepted

### What Was Inefficient

- ROADMAP and STATE lagged the phone for weeks (phases marked In Progress after ship)
- No milestone audit was run; closeout is an override
- Gradle hung for minutes when `dl.google.com` was blackholed

### Patterns Established

- Parsed article cache plus a Refresh article action instead of re-extracting every open
- Two locked reader error sentences; no library names on screen
- TTS start indexes precomputed off the main thread

### Key Lessons

1. Long 1.4.x patch trains still need a milestone close or GSD stays parked on UAT forever.
2. Main-thread regex over the whole article during composition is an ANR, not a parser crash.

### Cost Observations

- Sessions: many short 1.4.x ships plus three UAT close-outs on 2026-08-21
- Notable: device UAT sat pending from 2026-08-18 until daily-use accept

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.5 | many | 6 | Shipped as 1.4.x, archived after UAT accept |

### Cumulative Quality

| Milestone | Tests | Coverage | Zero-Dep Additions |
|-----------|-------|----------|-------------------|
| v1.5 | 640 JVM | n/a | jsoup + desugar for extract |

### Top Lessons (Verified Across Milestones)

1. Accept shipped UAT from daily use rather than leaving `human_needed` open.
