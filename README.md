# cloud-itonami-isic-7010

Open Business Blueprint for **ISIC Rev.5 7010**: Activities of head
offices.

This repository publishes a head-office actor -- unit intake, per-
jurisdiction transfer-pricing/group-reporting regulatory assessment,
and group budget/policy allocation finalization -- as an OSS business
that any qualified, licensed operator can fork, deploy, run, improve
and sell, so a community or independent professional never surrenders
customer data and ledgers to a closed SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320),
[`7210`](https://github.com/cloud-itonami/cloud-itonami-isic-7210),
[`7410`](https://github.com/cloud-itonami/cloud-itonami-isic-7410),
[`8710`](https://github.com/cloud-itonami/cloud-itonami-isic-8710),
[`8541`](https://github.com/cloud-itonami/cloud-itonami-isic-8541),
[`8690`](https://github.com/cloud-itonami/cloud-itonami-isic-8690),
[`9601`](https://github.com/cloud-itonami/cloud-itonami-isic-9601),
[`6420`](https://github.com/cloud-itonami/cloud-itonami-isic-6420),
[`7420`](https://github.com/cloud-itonami/cloud-itonami-isic-7420),
[`9609`](https://github.com/cloud-itonami/cloud-itonami-isic-9609),
[`8550`](https://github.com/cloud-itonami/cloud-itonami-isic-8550)) --
here it is **HeadOffice-LLM ⊣ Group Oversight Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a unit-
> intake summary, normalizing records, and checking whether a unit's
> own recorded transfer price and proposed allocation actually satisfy
> its own recorded arm's-length range and authorized limit -- but it
> has **no notion of which jurisdiction's transfer-pricing law is
> official, no license to finalize a real group budget/policy
> allocation, and no way to know on its own whether a transfer price
> has actually stayed within an arm's-length range**. Letting it
> finalize an allocation directly invites fabricated regulatory
> citations, a transfer price silently drifting outside its own
> permitted range, and an allocation silently exceeding its own
> authorized limit -- and liability, and tax/transfer-pricing risk,
> for whoever runs it. This project seals the HeadOffice-LLM into a
> single node and wraps it with an independent **Group Oversight
> Governor**, a human **approval workflow**, and an immutable **audit
> ledger**.

## Scope: what this actor does and does not do

This actor covers unit intake through transfer-pricing/group-
reporting regulatory assessment and group budget/policy allocation
finalization. It does **not**, by itself, hold any license required
to operate as a group head office in a given jurisdiction, and it
does not claim to. It also does not perform the actual strategic-
planning/budgeting work itself, or judge its quality --
`headoffice.registry`'s checks are pure ground-truth recomputes
against the unit's own recorded fields, not a strategic-planning
review. Whoever deploys and operates a live instance (a licensed
group head office) supplies any jurisdiction-specific license, the
real strategic-planning/budgeting work and the real group-reporting-
system integrations, and bears that jurisdiction's liability -- the
software supplies the governed, spec-cited, audited execution
scaffold so that operator does not have to build the compliance layer
from scratch.

### Actuation

**Finalizing a real group budget/policy allocation is never
autonomous, at any phase, by construction.** Two independent layers
enforce this (`headoffice.governor`'s `:actuation/finalize-
allocation` high-stakes gate and `headoffice.phase`'s phase table,
which never puts `:actuation/finalize-allocation` in any phase's
`:auto` set) -- see `headoffice.phase`'s docstring and `test/
headoffice/phase_test.clj`'s `finalize-allocation-never-auto-at-any-
phase`. The actor may draft, check and recommend; a human head-office
staff member is always the one who actually finalizes an allocation.
Matching `leasing`'s/`underwriting`'s/`testlab`'s/`clinic`'s/
`veterinary`'s/`funeral`'s/`parksafety`'s/`salon`'s/`entertainment`'s/
`facility`'s/`consulting`'s/`advertising`'s/`polling`'s/`research`'s/
`design`'s/`sports`'s/`alliedhealth`'s/`photo`'s/`personalservice`'s/
`edsupport`'s single-actuation shape, grounded directly in this
blueprint's own README text ("No automated proposal, by itself, can
complete the following without governor approval and audit evidence:
finalizing a group budget/policy allocation") -- a POSITIVE actuation
(finalizing a real record), matching this fleet's majority actuation
shape (`3600`/`6190` are the fleet's two NEGATIVE-actuation
exceptions).

## The core contract

```
unit intake + jurisdiction facts (headoffice.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ HeadOffice-LLM        │ ─────────────▶ │ Group Oversight               │  (independent system)
   │ (sealed)              │  + citations    │ Governor:                    │
   └───────────────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · transfer-price-      │
          │                         │ outside-arms-length-range           │
    record + ledger        escalate ┼ (range-bound, NEW) · budget-          │
          │              (ALWAYS for│ allocation-exceeds-authorized-         │
          │               :actuation│ limit (MAXIMUM-ceiling, reuse) ·        │
          │               /finalize-│ already-finalized                        │
          ▼               allocation)└───────────────────────┘
      human approval
```

**The HeadOffice-LLM never finalizes an allocation the Group
Oversight Governor would reject, and never does so without a human
sign-off.** Hard violations (fabricated regulatory requirements;
unsupported evidence; a transfer price outside its own arm's-length
range; a proposed allocation exceeding its own authorized limit; a
double finalization) force **hold** and *cannot* be approved past; a
clean finalization proposal still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a document-courier robot
moves physical unit-reporting packages between subsidiaries, under the
actor, gated by the independent **Group Oversight Governor**. The
governor never dispatches hardware itself; `:high`/`:safety-critical`
actions require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Group Oversight Governor, allocation-finalization draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`7010`). This vertical's unit records are practice-specific rather
than a shared cross-operator data contract, so `headoffice.*` runs on
the generic robotics/identity/forms/dmn/bpmn/audit-ledger stack only
-- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/headoffice/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + allocation-finalization history. No dynamically-filed sub-record -- the actuation op acts directly on a pre-seeded unit, and the double-actuation guard checks a dedicated `:allocation-finalized?` boolean rather than a `:status` value |
| `src/headoffice/registry.cljc` | Allocation-finalization draft records, plus `transfer-price-outside-arms-length-range?` -- the FIRST instance of a NEW range-bound check shape in this fleet (checks both an upper AND lower bound, unlike the existing single-bound MAXIMUM-ceiling/MINIMUM-threshold families) -- and `budget-allocation-exceeds-authorized-limit?`, an honest TENTH instance of the MAXIMUM-ceiling family, not claimed as new |
| `src/headoffice/facts.cljc` | Per-jurisdiction transfer-pricing/group-reporting catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/headoffice/headofficeadvisor.cljc` | **HeadOffice-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/report-verification/allocation-finalization proposals |
| `src/headoffice/governor.cljc` | **Group Oversight Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · transfer-price-outside-arms-length-range, GENUINELY NEW range-bound shape · budget-allocation-exceeds-authorized-limit, honest MAXIMUM-ceiling reuse · already-finalized guard) + 1 soft (confidence/actuation gate) |
| `src/headoffice/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (allocation finalization always human; unit intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/headoffice/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/headoffice/sim.cljc` | demo driver |
| `test/headoffice/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers unit intake through transfer-pricing/group-
reporting regulatory assessment and group budget/policy allocation
finalization -- the core governed lifecycle this blueprint's own
`docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Unit intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:unit/intake`/`:report/verify`) | Real group-reporting-system integration, real strategic-planning/budgeting work itself (see `headoffice.facts`'s docstring) |
| Allocation finalization, HARD-gated on full evidence, a transfer price within its own arm's-length range, and a proposed allocation within its own authorized limit, plus a double-finalization guard (`:actuation/finalize-allocation`) | Any strategic-planning/budgeting judgment itself -- deliberately outside this actor's competence |
| Immutable audit ledger for every intake/verification/finalization decision | |

Extending coverage is additive: add the next gate (e.g. an inter-unit
conflict-of-interest check) as its own governed op with its own HARD
checks and tests, following the SAME "an independent governor re-
verifies against the actor's own records before any real-world act"
pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`headoffice.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `headoffice.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `headoffice.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `HeadOffice-LLM` + `Group Oversight Governor` run as
real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, modeled closely on the sixty-
five prior actors' architecture. See `docs/adr/0001-architecture.md`
for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
