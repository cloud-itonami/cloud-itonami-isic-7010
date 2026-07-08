(ns headoffice.governor
  "Group Oversight Governor -- the independent compliance layer that
  earns the HeadOffice-LLM the right to commit. The LLM has no notion
  of jurisdictional transfer-pricing/group-reporting law, whether a
  unit's own recorded transfer price actually falls within its own
  recorded arm's-length range, whether a unit's own proposed
  allocation actually stays within its own recorded authorized limit,
  or when an act stops being a draft and becomes a real-world group
  budget/policy allocation, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD -- the head-office analog
  of `cloud-itonami-isic-8620`'s ClinicGovernor.

  Five checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence, a
  transfer price outside its own arm's-length range, an allocation
  exceeding its own authorized limit, or a double finalization). The
  confidence/actuation gate is SOFT: it asks a human to look (low
  confidence / actuation), and the human may approve -- but see
  `headoffice.phase`: for `:stake :actuation/finalize-allocation` (a
  real group budget/policy allocation) NO phase ever allows auto-
  commit either. Two independent layers agree that actuation is
  always a human call.

    1. Spec-basis                  -- did the report proposal cite an
                                       OFFICIAL source (`headoffice.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/finalize-
                                       allocation`, has the unit
                                       actually been reported with a
                                       full unit-report-record/
                                       budget-authorization-record/
                                       transfer-price-benchmark-
                                       record/allocation-completion-
                                       record evidence checklist on
                                       file?
    3. Transfer price outside
       arm's-length range          -- for `:actuation/finalize-
                                       allocation`, INDEPENDENTLY
                                       recompute whether the unit's
                                       own recorded transfer price
                                       falls outside its own recorded
                                       arm's-length range on EITHER
                                       side (`headoffice.registry/
                                       transfer-price-outside-arms-
                                       length-range?`) -- needs no
                                       proposal inspection at all. The
                                       FIRST instance of this fleet's
                                       NEW range-bound check shape,
                                       grounded in real transfer-
                                       pricing law (OECD Transfer
                                       Pricing Guidelines' arm's-
                                       length principle, US IRC §482,
                                       Japan's 租税特別措置法第66条の
                                       4, Germany's AStG §1).
    4. Budget allocation exceeds
       authorized limit            -- for `:actuation/finalize-
                                       allocation`, INDEPENDENTLY
                                       recompute whether the unit's
                                       own proposed allocation amount
                                       exceeds its own recorded
                                       authorized allocation limit
                                       (`headoffice.registry/budget-
                                       allocation-exceeds-authorized-
                                       limit?`) -- an HONEST reuse of
                                       this fleet's MAXIMUM-ceiling
                                       check family (facility/school/
                                       card/recovery/care/navigator/
                                       advertising/nursing/holdco
                                       established the first nine;
                                       this is the TENTH), closely
                                       analogous to `advertising.
                                       registry/media-spend-exceeds-
                                       authorized-budget?`'s own
                                       proposed-spend/authorized-
                                       budget shape, not claimed as
                                       new.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       finalize-allocation` (a REAL
                                       group budget/policy allocation)
                                       -> escalate.

  One more guard, double-finalization prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-finalized-violations` refuses to
  finalize an allocation for the SAME unit twice, off a dedicated
  `:allocation-finalized?` fact (never a `:status` value) -- the SAME
  'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [headoffice.facts :as facts]
            [headoffice.registry :as registry]
            [headoffice.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Finalizing a real group budget/policy allocation is the ONE real-
  world actuation event this actor performs -- a single-member set,
  matching `cloud-itonami-isic-6511`'s/`6621`'s/`6629`'s/`6612`'s/
  `6492`'s/`7120`'s/`8620`'s/`personalservice`'s/`edsupport`'s single-
  actuation shape."
  #{:actuation/finalize-allocation})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:report/verify` (or `:actuation/finalize-allocation`) proposal
  with no spec-basis citation is a HARD violation -- never invent a
  jurisdiction's group-reporting/transfer-pricing requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:report/verify :actuation/finalize-allocation} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は配分基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/finalize-allocation`, the jurisdiction's required
  unit-report-record/budget-authorization-record/transfer-price-
  benchmark-record/allocation-completion-record evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-allocation)
    (let [u (store/unit st subject)
          report (store/report-of st subject)]
      (when-not (and report
                     (facts/required-evidence-satisfied?
                      (:jurisdiction u) (:checklist report)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(ユニット報告記録/予算承認記録/移転価格ベンチマーク記録/配分完了記録等)が充足していない状態での提案"}]))))

(defn- transfer-price-outside-arms-length-range-violations
  "For `:actuation/finalize-allocation`, INDEPENDENTLY recompute
  whether the unit's own recorded transfer price falls outside its
  own recorded arm's-length range via `headoffice.registry/transfer-
  price-outside-arms-length-range?` -- needs no proposal inspection
  at all, since its inputs are permanent ground-truth fields already
  on the unit."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-allocation)
    (let [u (store/unit st subject)]
      (when (registry/transfer-price-outside-arms-length-range? u)
        [{:rule :transfer-price-outside-arms-length-range
          :detail (str subject " の移転価格(" (:transfer-price u)
                      ")が独立企業間価格レンジ(" (:arms-length-range-min u) "-" (:arms-length-range-max u) ")の範囲外")}]))))

(defn- budget-allocation-exceeds-authorized-limit-violations
  "For `:actuation/finalize-allocation`, INDEPENDENTLY recompute
  whether the unit's own proposed allocation amount exceeds its own
  recorded authorized allocation limit via `headoffice.registry/
  budget-allocation-exceeds-authorized-limit?` -- needs no proposal
  inspection at all."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-allocation)
    (let [u (store/unit st subject)]
      (when (registry/budget-allocation-exceeds-authorized-limit? u)
        [{:rule :budget-allocation-exceeds-authorized-limit
          :detail (str subject " の提案配分額(" (:proposed-allocation-amount u)
                      ")が承認限度額(" (:authorized-allocation-limit u) ")を超過")}]))))

(defn- already-finalized-violations
  "For `:actuation/finalize-allocation`, refuses to finalize an
  allocation for the SAME unit twice, off a dedicated `:allocation-
  finalized?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-allocation)
    (when (store/unit-already-finalized? st subject)
      [{:rule :already-finalized
        :detail (str subject " は既に配分確定済み")}])))

(defn check
  "Censors a HeadOffice-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (transfer-price-outside-arms-length-range-violations request st)
                           (budget-allocation-exceeds-authorized-limit-violations request st)
                           (already-finalized-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
