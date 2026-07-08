# ADR-0001: HeadOffice-LLM ⊣ Group Oversight Governor architecture

## Status

Accepted. `cloud-itonami-isic-7010` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-7010` publishes an OSS business blueprint for
activities of head offices: overseeing and administering other units
of the same enterprise or group (strategic planning, budgeting,
group-wide policy). Like every prior actor in this fleet, the
blueprint alone is not an implementation: this ADR records the
governed-actor architecture that promotes it to real, tested code,
following the same langgraph StateGraph + independent Governor +
Phase 0→3 rollout pattern established by `cloud-itonami-isic-6511`
(life insurance) and applied across sixty-five prior siblings, most
recently `cloud-itonami-isic-8550` (educational support activities).

## Decision

### Decision 1: single-actuation shape

This blueprint's own README/business-model.md/operator-guide.md
consistently name only ONE real-world act: "finalizing a group
budget/policy allocation." Matching `leasing`/`underwriting`/
`testlab`/`clinic`/`veterinary`/`funeral`/`parksafety`/`salon`/
`entertainment`/`facility`/`consulting`/`advertising`/`polling`/
`research`/`design`/`sports`/`alliedhealth`/`photo`/
`personalservice`/`edsupport`'s single-actuation shape, `high-stakes`
here is a one-member set, `#{:actuation/finalize-allocation}`.

### Decision 2: entity and op shape

The primary entity is a `unit` (subsidiary/business unit), matching
the business-model.md's own Offer language ("subsidiary/unit
reporting intake"). Three ops: `:unit/intake` (directory upsert, no
capital risk), `:report/verify` (per-jurisdiction transfer-pricing/
group-reporting evidence checklist, never auto), and `:actuation/
finalize-allocation` (POSITIVE, high-stakes -- finalizing a real
group budget/policy allocation). Unlike most siblings, this build has
no dedicated screening op: both distinctive checks are ground-truth
numeric recomputes scoped to the actuation, matching `advertising`/
7310's and `navigator`/8691's precedent for ground-truth checks that
need no separate screening op.

### Decision 3: `transfer-price-outside-arms-length-range?` -- the first instance of a new range-bound check shape

Before writing this check, every prior sibling's governor/registry
namespaces were grepped for "transfer-pric", "arms-length" and
"arm's-length" -- zero hits, confirming this is a genuinely new
concept, avoiding the false-precedent-claim risk `leasing`'s ADR-0001
documents. `headoffice.registry/transfer-price-outside-arms-length-
range?` recomputes `(or (< transfer-price range-min) (> transfer-
price range-max))` directly from the unit's own recorded fields --
unlike this fleet's existing single-bound MAXIMUM-ceiling family (9
prior instances) or MINIMUM-threshold family (8 prior instances),
this is a two-sided range check, the FIRST such instance in this
fleet. It reuses the SAME "ground-truth recompute against the
entity's own permanent fields" discipline both existing bound
families already establish. Grounded in OECD Transfer Pricing
Guidelines' arm's-length principle, US IRC §482, Japan's 租税特別措置
法第66条の4, and Germany's AStG §1. Gates only `:actuation/finalize-
allocation`.

### Decision 4: `budget-allocation-exceeds-authorized-limit?` -- an honest tenth MAXIMUM-ceiling instance, not claimed as new

`facility`/`school`/`card`/`recovery`/`care`/`navigator`/
`advertising`/`nursing`/`holdco` established the first nine instances
of this fleet's MAXIMUM-ceiling check family. `headoffice.registry/
budget-allocation-exceeds-authorized-limit?` is the TENTH, applying
the same ceiling-only comparison to a unit's own proposed allocation
amount against its own recorded authorized limit -- closely analogous
to `advertising.registry/media-spend-exceeds-authorized-budget?`'s
own proposed-spend/authorized-budget shape, not claimed as new. Gates
only `:actuation/finalize-allocation`.

### Decision 5: dedicated double-actuation-guard boolean

`:allocation-finalized?` is a dedicated boolean on the `unit` record,
never a single `:status` value -- the same discipline every prior
sibling governor's guards establish, informed by `cloud-itonami-
isic-6492`'s status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`headoffice.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in `test/headoffice/
store_contract_test.clj` -- the same seam every sibling actor uses so
swapping the SSoT backend is a configuration change, not a rewrite.
The protocol's per-entity accessor is named `unit` directly -- not a
Clojure special form, so no `-of` suffix workaround was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:unit/intake` (no
capital risk). `:report/verify` is never auto-eligible at any phase
(matching every sibling's verification-op posture), and `:actuation/
finalize-allocation` is permanently excluded from every phase's
`:auto` set -- a structural fact, not a rollout milestone, enforced
by BOTH `headoffice.phase` and `headoffice.governor`'s `high-stakes`
set independently.

### Decision 8: no bespoke domain capability lib

This blueprint's own `:itonami.blueprint/required-technologies` names
no domain-specific capability beyond the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack -- there was no capability-lib
decision to make at all.

### Decision 9: mock + LLM advisor pair

`headoffice.headofficeadvisor` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
finalizing an allocation).

### Decision 10: no `blueprint.edn` field-sync fixes needed

Matching `photo`/7420's, `personalservice`/9609's and `edsupport`/
8550's own experience, this repo's `blueprint.edn` already had the
correct `isic-` prefixed `:id` and correctly populated `:required-
technologies`/`:optional-technologies` matching the `kotoba-lang/
industry` registry's own entry for `"7010"` exactly -- only the
`:maturity` field itself needed adding.

## Alternatives considered

- **A dual-actuation shape** (e.g. splitting "budget" and "policy"
  allocation into two acts). Rejected: the blueprint's own text
  consistently names only ONE real-world act ("finalizing a group
  budget/policy allocation"); inventing a second would not be
  grounded in the blueprint's own text.
- **Framing `transfer-price-outside-arms-length-range?` as a
  MAXIMUM-ceiling-family reuse** (checking only the upper bound).
  Rejected: real transfer-pricing law requires a price to fall
  WITHIN a range on both sides, not merely under a ceiling -- a
  single-bound characterization would misrepresent the real-world
  compliance requirement this check exists to enforce.
- **A dedicated screening op for either distinctive check.**
  Rejected: both checks are pure ground-truth recomputes against
  permanent unit fields needing no proposal inspection, matching
  `advertising`/7310's and `navigator`/8691's precedent that such
  checks are scoped directly to the actuation, with no separate
  screening op required.

## Consequences

- Sixty-sixth actor in this fleet (65 implemented before this
  build).
- Introduces a genuinely NEW range-bound check shape (first instance,
  grep-verified absent from every prior sibling), distinct from both
  existing single-bound check families.
- Documents an honest TENTH instance of the MAXIMUM-ceiling check
  family, not claimed as new.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/headoffice/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself.

## References

- `orgs/cloud-itonami/cloud-itonami-isic-7010/README.md`
- `orgs/cloud-itonami/cloud-itonami-isic-7010/docs/business-model.md`
- `orgs/kotoba-lang/industry/resources/kotoba/industry/registry.edn` (entry `"7010"`)
