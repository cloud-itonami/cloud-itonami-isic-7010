(ns headoffice.phase
  "Phase 0->3 staged rollout -- the head-office analog of
  `cloud-itonami-isic-8620`'s `clinic.phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- unit intake allowed, every write
                                 needs human approval.
    Phase 2  assisted-verify  -- adds group-report verification
                                 writes, still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:unit/intake` (no capital risk yet)
                                 may auto-commit. `:actuation/
                                 finalize-allocation` NEVER auto-
                                 commits, at any phase.

  `:actuation/finalize-allocation` is deliberately ABSENT from every
  phase's `:auto` set, including phase 3 -- a permanent structural
  fact, not a rollout milestone still to come. Finalizing a real
  group budget/policy allocation is the ONE real-world act this actor
  performs; it is always a human head-office-staff call.
  `headoffice.governor`'s `:actuation/finalize-allocation` high-
  stakes gate enforces the same invariant independently -- two
  layers, not one, agree on this. Phase 3's `:auto` set here has only
  ONE member (`:unit/intake`) -- this domain has no separate no-
  capital-risk 'file' lifecycle distinct from the unit itself.")

(def read-ops  #{})
(def write-ops #{:unit/intake :report/verify :actuation/finalize-allocation})

;; NOTE the invariant: `:actuation/finalize-allocation` is a member of
;; `write-ops` (governor-gated like any write) but is NEVER a member
;; of any phase's `:auto` set below. Do not add it there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"        :writes #{}                                                       :auto #{}}
   1 {:label "assisted-intake"  :writes #{:unit/intake}                                            :auto #{}}
   2 {:label "assisted-verify"  :writes #{:unit/intake :report/verify}                             :auto #{}}
   3 {:label "supervised-auto"  :writes write-ops
      :auto #{:unit/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:actuation/finalize-allocation` is never auto-eligible at any
    phase, so it always escalates once the governor clears it (or
    holds if the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a Group Oversight Governor verdict to a base disposition before
  the phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
