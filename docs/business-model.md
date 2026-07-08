# Business Model: Activities of head offices

## Classification

- Repository: `cloud-itonami-isic-7010`
- ISIC Rev.5: `7010`
- Activity: head-office activities -- overseeing and administering other units of the same enterprise or group (strategic planning, budgeting, group-wide policy)
- Social impact: professional standards, data sovereignty, transparent audit

## Customer

- multi-unit cooperatives
- group holding administrators
- franchise head-office operators

## Offer

- subsidiary/unit reporting intake
- group-budget/policy proposal
- inter-unit resource-allocation proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per group entity
- support: monthly retainer with SLA
- migration: import from an incumbent group-reporting system
- budget-cycle processing fee

## Trust Controls

- no group-wide policy or budget allocation is finalized without human sign-off
- a fabricated unit report forces a hold, not an override
- every allocation path is auditable
- emergency manual override paths remain outside LLM control
- a transfer price falling outside a unit's own recorded arm's-length
  range, or a proposed allocation exceeding a unit's own recorded
  authorized limit, forces a hold, not an override
- allocation finalization is logged and escalated, and cannot be
  finalized twice for the same unit: a double-finalization attempt is
  held off this actor's own unit facts alone, with no upstream
  comparison needed

## Group Oversight Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:group-
oversight-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (finalizing a
real group budget/policy allocation) must pass. The governor sits
between the HeadOffice-LLM and execution, per the README's Core
Contract:

```text
HeadOffice-LLM -> Group Oversight Governor -> hold, proceed, or human approval
```

**Approves**: routine head-office actions proposed against a unit
that already has a consented group report on file, satisfied
required evidence, a transfer price within its own recorded arm's-
length range, and a proposed allocation within its own recorded
authorized limit. These proceed straight to the unit ledger.

**Rejects or escalates**: the governor refuses to let the advisor
finalize an allocation on its own authority when any of the following
hold -- a fabricated jurisdiction spec-basis; incomplete evidence; a
transfer price outside its own arm's-length range; a proposed
allocation exceeding its own authorized limit; a double-finalization
attempt. A clean finalization proposal still always routes to a
human -- `:actuation/finalize-allocation` is never auto-committed, at
any rollout phase.
