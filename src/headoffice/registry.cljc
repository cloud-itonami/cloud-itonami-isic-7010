(ns headoffice.registry
  "Pure-function group-budget/policy-allocation record construction --
  an append-only head-office book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for an allocation reference
  number -- every group/jurisdiction assigns its own reference
  format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `headoffice.facts` uses.

  Two distinctive ground-truth checks:

  `transfer-price-outside-arms-length-range?` is the FIRST instance of
  a NEW 'range-bound' check shape in this fleet (grep-verified absent
  -- no 'transfer-pric'/'arms-length'/'arm's-length' concept exists
  anywhere else in this fleet): unlike this fleet's existing MAXIMUM-
  ceiling family (a single upper bound, 9 prior instances) or MINIMUM-
  threshold family (a single lower bound, 8 prior instances), this
  check recomputes whether a unit's own recorded inter-unit transfer
  price falls OUTSIDE its own recorded arm's-length range on EITHER
  side (below the range minimum OR above the range maximum) -- a
  direct, natural mapping onto real transfer-pricing law (OECD
  Transfer Pricing Guidelines' arm's-length principle, US IRC §482,
  Japan's 租税特別措置法第66条の4, Germany's AStG §1). It reuses the
  SAME 'ground-truth recompute against the entity's own permanent
  fields, no upstream comparison needed' discipline both existing
  bound families already establish, just with two bounds instead of
  one.

  `budget-allocation-exceeds-authorized-limit?` is an HONEST reuse of
  the MAXIMUM-ceiling check family shape (`facility.registry/
  occupancy-exceeds-capacity?`/`school.registry/class-size-exceeds-
  maximum?`/`card.registry/settlement-amount-exceeds-authorized?`/
  `recovery.registry/contamination-percentage-exceeds-maximum?`/
  `care.registry/caregiver-workload-exceeds-maximum?`/`navigator.
  registry/eligibility-window-elapsed-exceeds-validity?`/
  `advertising.registry/media-spend-exceeds-authorized-budget?`/
  `nursing`'s/`holdco`'s instances -- the TENTH instance overall),
  applying the SAME ceiling-only comparison to a unit's own proposed
  allocation amount against its own recorded authorized limit --
  closely analogous to `advertising.registry/media-spend-exceeds-
  authorized-budget?`'s own proposed-spend/authorized-budget shape,
  not claimed as new.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real group-reporting system. It builds the RECORD a
  head-office operator would keep, not the act of finalizing the
  allocation itself (that is `headoffice.operation`'s `:actuation/
  finalize-allocation`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  head-office operator's own act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn transfer-price-outside-arms-length-range?
  "Does `unit`'s own recorded `:transfer-price` fall outside its own
  recorded `:arms-length-range-min`/`:arms-length-range-max`? A pure
  ground-truth check against the unit's own permanent fields -- no
  upstream comparison needed. The FIRST instance of this fleet's
  range-bound check shape (see ns docstring)."
  [{:keys [transfer-price arms-length-range-min arms-length-range-max]}]
  (and (number? transfer-price) (number? arms-length-range-min) (number? arms-length-range-max)
       (or (< transfer-price arms-length-range-min)
           (> transfer-price arms-length-range-max))))

(defn budget-allocation-exceeds-authorized-limit?
  "Does `unit`'s own `:proposed-allocation-amount` exceed its own
  recorded `:authorized-allocation-limit`? A pure ground-truth check
  against the unit's own permanent fields -- no upstream comparison
  needed. The TENTH instance of this fleet's MAXIMUM-ceiling check
  family (see ns docstring), not claimed as new."
  [{:keys [proposed-allocation-amount authorized-allocation-limit]}]
  (and (number? proposed-allocation-amount) (number? authorized-allocation-limit)
       (> proposed-allocation-amount authorized-allocation-limit)))

(defn register-allocation-finalization
  "Validate + construct the ALLOCATION-FINALIZATION registration DRAFT
  -- the head-office operator's own act of finalizing a real group
  budget/policy allocation. Pure function -- does not touch any real
  group-reporting system; it builds the RECORD an operator would
  keep. `headoffice.governor` independently re-verifies the unit's
  own transfer-price range and authorized-limit ground truths, and
  blocks a double-finalization for the same unit, before this is ever
  allowed to commit."
  [unit-id jurisdiction sequence]
  (when-not (and unit-id (not= unit-id ""))
    (throw (ex-info "allocation-finalization: unit_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "allocation-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "allocation-finalization: sequence must be >= 0" {})))
  (let [allocation-number (str (str/upper-case jurisdiction) "-ALC-" (zero-pad sequence 6))
        record {"record_id" allocation-number
                "kind" "allocation-finalization-draft"
                "unit_id" unit-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "allocation_number" allocation-number
     "certificate" (unsigned-certificate "AllocationFinalization" allocation-number allocation-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
