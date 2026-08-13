(ns headoffice.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 for this repo: before this
  namespace existed there was NO demo page and no generator at all
  (`docs/` held only prose + `index.html`). Nothing here is
  hand-written page content -- every unit row, every hold, every
  allocation record and every gate cell is read back out of a REAL
  run of this repo's own stack:

      headoffice.operation  (langgraph StateGraph, `g/run*`)
        -> headoffice.headofficeadvisor  (contained advisor node)
        -> headoffice.governor           (Group Oversight Governor)
        -> headoffice.phase              (rollout gate)
        -> headoffice.store              (SSoT + append-only ledger)

  The gate tables are derived from `headoffice.phase/phases`,
  `headoffice.phase/write-ops`, `headoffice.governor/high-stakes` and
  `headoffice.governor/confidence-floor` -- i.e. from the code that
  actually decides, not from a prose copy of it that can drift. The
  jurisdiction table is derived from `headoffice.facts/catalog` and
  `headoffice.facts/coverage`.

  Determinism: the page contains no timestamps and no run ids; the
  scenario is fixed and the seed store is deterministic, so two runs
  produce byte-identical output (verified by diffing two runs into
  separate scratch files).

  Build-time invariant: `-main` THROWS unless the run this build
  performed actually produced HARD governor holds, and unless every
  hard rule the governor emitted is present in the rendered document.
  A console that quietly renders zero holds would be indistinguishable
  from a console whose governor never fired.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [jp-go-dds.skin]
            [langgraph.graph :as g]
            [headoffice.facts :as facts]
            [headoffice.governor :as governor]
            [headoffice.operation :as op]
            [headoffice.phase :as phase]
            [headoffice.store :as store]))

;; ----------------------------- the real run -----------------------------

(def ^:private operator
  "Phase-3 head-office staff -- the same operator context
  `headoffice.sim` uses."
  {:actor-id "op-1" :actor-role :head-office-staff :phase 3})

(def ^:private phase-1-operator
  "The SAME operator earlier in the rollout. Used once, to show that
  the phase gate is a second, independent layer: an op the governor
  would clear is still held when the phase does not enable that write."
  (assoc operator :phase 1))

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Drives a freshly seeded store (`headoffice.store/seed-db`, units
  `unit-1`..`unit-4`) through every disposition this actor can reach.
  Subjects, names, jurisdictions, prices and limits are ALL from
  `store/demo-data` -- nothing is invented here.

  unit-1 (JPN, Sato Manufacturing K.K., transfer price 100 inside
  [90,110], proposed 500000 within limit 1000000) walks the full clean
  lifecycle: intake auto-commits at phase 3, a group-report
  verification escalates (phase 3 enables the write but never
  auto-commits it) and is approved, and the allocation finalization
  escalates -- `:actuation/finalize-allocation` is absent from EVERY
  phase's `:auto` set AND is `governor/high-stakes`, two independent
  layers agreeing -- and is approved, producing a real
  `headoffice.registry` allocation record.

  Then five HARD governor holds, none of which ever reach a human:

    unit-1  `:already-finalized`   -- finalizing the same unit twice.
    unit-2  `:no-spec-basis`       -- a report checklist proposed for a
                                      jurisdiction (ATL) with no entry
                                      in `headoffice.facts/catalog`.
    unit-2  `:evidence-incomplete` -- finalizing with no committed
                                      report on file (its verify was
                                      held above, so nothing was filed).
    unit-3  `:transfer-price-outside-arms-length-range`
                                   -- recorded transfer price 150 lies
                                      outside its own recorded
                                      arm's-length range [90,110].
    unit-4  `:budget-allocation-exceeds-authorized-limit`
                                   -- proposed allocation 1500000
                                      exceeds its own recorded
                                      authorized limit 1000000.

  Finally one PHASE hold (not a governor violation): the same clean
  `:report/verify` replayed against a phase-1 context is refused
  because phase 1 does not enable that write at all.

  Returns the store. Everything rendered below is read back from it."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    ;; -- unit-1: clean end-to-end lifecycle --------------------------
    (exec! actor "u1-intake"
           {:op :unit/intake :subject "unit-1"
            :patch {:id "unit-1" :unit-name "Sato Manufacturing K.K."}}
           operator)

    (exec! actor "u1-verify" {:op :report/verify :subject "unit-1"} operator)
    (approve! actor "u1-verify")

    (exec! actor "u1-finalize" {:op :actuation/finalize-allocation :subject "unit-1"} operator)
    (approve! actor "u1-finalize")

    ;; -- HARD: double finalization -----------------------------------
    (exec! actor "u1-finalize-again" {:op :actuation/finalize-allocation :subject "unit-1"} operator)

    ;; -- HARD: no spec-basis for the jurisdiction --------------------
    (exec! actor "u2-verify" {:op :report/verify :subject "unit-2" :no-spec? true} operator)

    ;; -- HARD: evidence checklist never filed ------------------------
    (exec! actor "u2-finalize" {:op :actuation/finalize-allocation :subject "unit-2"} operator)

    ;; -- HARD: transfer price outside its own arm's-length range -----
    (exec! actor "u3-verify" {:op :report/verify :subject "unit-3"} operator)
    (approve! actor "u3-verify")
    (exec! actor "u3-finalize" {:op :actuation/finalize-allocation :subject "unit-3"} operator)

    ;; -- HARD: allocation over its own authorized limit --------------
    (exec! actor "u4-verify" {:op :report/verify :subject "unit-4"} operator)
    (approve! actor "u4-verify")
    (exec! actor "u4-finalize" {:op :actuation/finalize-allocation :subject "unit-4"} operator)

    ;; -- PHASE hold: same clean op, earlier rollout phase ------------
    (exec! actor "u1-verify-phase1" {:op :report/verify :subject "unit-1"} phase-1-operator)
    db))

;; ----------------------------- ledger views -----------------------------

(defn- holds
  "Every `:governor-hold` fact the run produced."
  [ledger]
  (filter #(= :governor-hold (:t %)) ledger))

(defn- hard-holds
  "Holds carrying at least one governor VIOLATION -- i.e. a HARD hold
  no approver can override, as opposed to a phase-gate hold, which
  carries `:phase-reason` and an empty violation vector."
  [ledger]
  (filter #(seq (:violations %)) (holds ledger)))

(defn- phase-holds [ledger]
  (filter #(and (empty? (:violations %)) (:phase-reason %)) (holds ledger)))

(defn- hard-rules
  "Distinct hard rules, in first-observed order."
  [ledger]
  (distinct (mapcat #(map :rule (:violations %)) (hard-holds ledger))))

(defn- last-fact-for [ledger subject]
  (last (filter #(= (:subject %) subject) ledger)))

;; ----------------------------- approver retention -----------------------------
;;
;; Measured, not assumed. `headoffice.operation`'s approval node puts
;; the approver on the record's `:payload` (`:approved-by`), leaving
;; `:value` untouched. Whether that survives depends entirely on which
;; key each `commit-record!` branch reads -- and the branches differ.
;; So instead of asserting anything, look in the register the commit
;; actually wrote and report what is there.

(defn- approver-key
  "Any key on `m` that names an approver, or nil. Works across the
  keyword-keyed registers (`:approved-by`) and the string-keyed
  registry records."
  [m]
  (when (map? m)
    (first (filter #(str/includes? (str/lower-case (name %)) "approv") (keys m)))))

(defn- register-for
  "The SSoT register the commit for `op`/`subject` actually wrote."
  [db op subject]
  (case op
    :report/verify (store/report-of db subject)
    :unit/intake (store/unit db subject)
    :actuation/finalize-allocation
    (first (filter #(= subject (get % "unit_id")) (store/allocation-history db)))
    nil))

(defn- approval-provenance
  "For every approval this run granted, join the audit fact to the
  register the commit wrote and DERIVE whether the approver identity
  is retrievable from the SSoT, or only from the audit ledger."
  [db ledger]
  (for [{:keys [op subject by]} (filter #(= :approval-granted (:t %)) ledger)
        :let [reg (register-for db op subject)
              k (approver-key reg)]]
    {:op op :subject subject :approver by
     :register (case op
                 :report/verify "report-of"
                 :unit/intake "unit"
                 :actuation/finalize-allocation "allocation-history"
                 "-")
     :retained? (some? k)
     :retained-as k
     :retained-value (when k (get reg k))}))

;; ----------------------------- html -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- kw [v] (if (keyword? v) (name v) (str v)))

(defn- code [v] (str "<code>" (esc (kw v)) "</code>"))

(defn- ok [s] (str "<span class=\"ok\">" s "</span>"))
(defn- warn [s] (str "<span class=\"warn\">" s "</span>"))
(defn- crit [s] (str "<span class=\"critical\">" s "</span>"))
(defn- muted [s] (str "<span class=\"muted\">" s "</span>"))

(defn- row [& cells]
  (str "        <tr>" (str/join (map #(str "<td>" % "</td>") cells)) "</tr>"))

(defn- table [headers rows]
  (str "    <table>\n"
       "      <thead><tr>" (str/join (map #(str "<th>" (esc %) "</th>") headers)) "</tr></thead>\n"
       "      <tbody>\n"
       (str/join "\n" rows) "\n"
       "      </tbody>\n"
       "    </table>\n"))

(defn- section [title lead body]
  (str "  <section class=\"card\">\n"
       "    <h2>" (esc title) "</h2>\n"
       (when lead (str "    <p class=\"muted\">" lead "</p>\n"))
       body
       "  </section>\n"))

;; -- 1. units ---------------------------------------------------------

(defn- status-cell [ledger subject]
  (let [f (last-fact-for ledger subject)]
    (case (:t f)
      nil (muted "no activity")
      :committed (ok (str "committed &middot; " (esc (kw (:op f)))))
      :approval-granted (ok (str "approved &amp; committed &middot; " (esc (kw (:op f)))))
      :governor-hold (if (seq (:violations f))
                       (crit (str "HARD hold &middot; " (esc (kw (:rule (first (:violations f)))))))
                       (warn (str "phase hold &middot; " (esc (kw (:phase-reason f))))))
      (muted (esc (kw (:t f)))))))

(defn- unit-rows [db ledger]
  (for [{:keys [id unit-name jurisdiction transfer-price
                arms-length-range-min arms-length-range-max
                proposed-allocation-amount authorized-allocation-limit
                allocation-finalized? allocation-number]} (store/all-units db)]
    (row (code id)
         (esc unit-name)
         (esc jurisdiction)
         (let [out? (or (< transfer-price arms-length-range-min)
                        (> transfer-price arms-length-range-max))]
           (str (esc transfer-price) " "
                ((if out? crit ok)
                 (str "[" (esc arms-length-range-min) "&ndash;" (esc arms-length-range-max) "]"))))
         (let [over? (> proposed-allocation-amount authorized-allocation-limit)]
           (str (esc proposed-allocation-amount) " / "
                ((if over? crit ok) (esc authorized-allocation-limit))))
         (if allocation-finalized?
           (ok (str "finalized &middot; <code>" (esc allocation-number) "</code>"))
           (muted "not finalized"))
         (status-cell ledger id))))

;; -- 2. holds ---------------------------------------------------------

(defn- hold-rows [ledger]
  (for [h (hard-holds ledger)
        v (:violations h)]
    (row (crit "HARD")
         (code (:rule v))
         (code (:op h))
         (code (:subject h))
         (esc (:detail v))
         (esc (:confidence h)))))

(defn- phase-hold-rows [ledger]
  (for [h (phase-holds ledger)]
    (row (warn "PHASE")
         (code (:phase-reason h))
         (code (:op h))
         (code (:subject h))
         (esc (str "phase " (:phase h) " ("
                   (:label (get phase/phases (:phase h))) ") does not enable this write"))
         (esc (:confidence h)))))

;; -- 3. gates (derived from phase/governor, not transcribed) ----------

(defn- op-gate-rows []
  (for [o (sort-by kw phase/write-ops)]
    (let [writes-at (sort (keep (fn [[p {:keys [writes]}]] (when (writes o) p)) phase/phases))
          auto-at (sort (keep (fn [[p {:keys [auto]}]] (when (auto o) p)) phase/phases))]
      (row (code o)
           (if (seq writes-at) (esc (str/join ", " writes-at)) (muted "none"))
           (if (seq auto-at)
             (ok (esc (str/join ", " auto-at)))
             (crit "never &mdash; no phase auto-commits this op"))
           (if (governor/high-stakes o)
             (crit "high-stakes &middot; always human")
             (muted "not high-stakes"))))))

(defn- phase-rows []
  (for [[p {:keys [label writes auto]}] (sort-by key phase/phases)]
    (row (esc p)
         (esc label)
         (if (seq writes) (str/join " " (map code (sort-by kw writes))) (muted "none"))
         (if (seq auto) (str/join " " (map code (sort-by kw auto))) (muted "none")))))

;; -- 4. jurisdictions -------------------------------------------------

(defn- jurisdiction-rows [db]
  (let [used (frequencies (map :jurisdiction (store/all-units db)))]
    (for [iso3 (sort (distinct (concat (keys facts/catalog) (keys used))))]
      (let [sb (facts/spec-basis iso3)]
        (row (code iso3)
             (if sb (esc (:name sb)) (crit "not in catalog"))
             (if sb (esc (:owner-authority sb)) (muted "&mdash;"))
             (if sb (esc (:legal-basis sb)) (muted "&mdash;"))
             (if sb (esc (count (:required-evidence sb))) (muted "0"))
             (esc (get used iso3 0)))))))

;; -- 5. approval provenance -------------------------------------------

(defn- provenance-rows [db ledger]
  (for [{:keys [op subject approver register retained? retained-as retained-value]}
        (approval-provenance db ledger)]
    (row (code op)
         (code subject)
         (esc approver)
         (code register)
         (if retained?
           (ok (str "retained as <code>" (esc (kw retained-as)) "</code> = "
                    (esc retained-value)))
           (warn "audit only &mdash; not retained in record")))))

;; -- 6. ledger + records ----------------------------------------------

(defn- ledger-rows [ledger]
  (map-indexed
   (fn [i {:keys [t op subject disposition basis summary phase-reason]}]
     (row (esc i)
          (case t
            :committed (ok (esc (kw t)))
            :governor-hold (crit (esc (kw t)))
            :approval-granted (ok (esc (kw t)))
            (esc (kw t)))
          (code op)
          (code subject)
          (esc (kw (or disposition "")))
          (esc (or (some->> basis (map kw) (str/join ", "))
                   (some-> phase-reason kw)
                   summary
                   ""))))
   ledger))

(defn- allocation-rows [db]
  (for [r (store/allocation-history db)]
    (row (code (get r "record_id"))
         (code (get r "unit_id"))
         (esc (get r "jurisdiction"))
         (esc (get r "kind"))
         (if (get r "immutable") (ok "immutable") (warn "mutable")))))

;; ----------------------------- document -----------------------------

(defn render
  "Renders the whole document from a store that has already been driven
  by `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        cov (facts/coverage (distinct (map :jurisdiction (store/all-units db))))]
    (str
     "<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">\n"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
     "<title>cloud-itonami-isic-7010 &middot; head-office (activities of head offices)</title>\n"
     "<style>" (jp-go-dds.skin/dds+skin) "</style>\n"
     "</head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Activities of head offices (ISIC 7010) &mdash; Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample &middot; build-time generated from a real actor run &middot; allocation finalization is always human-approved</span>\n"
     "</header>\n"
     "<main>\n"

     (section "Group units"
              (str "Seeded from <code>headoffice.store/demo-data</code>. "
                   "Transfer price is shown against the unit's own recorded arm's-length range, "
                   "and the proposed allocation against its own recorded authorized limit &mdash; "
                   "both are what <code>headoffice.governor</code> independently recomputes.")
              (table ["Unit" "Name" "Jurisdiction" "Transfer price / arm's-length range"
                      "Proposed allocation / authorized limit" "Allocation" "Last op"]
                     (unit-rows db ledger)))

     (section "HARD governor holds this run"
              (str "Emitted by <code>headoffice.governor/check</code>. A HARD hold cannot be "
                   "overridden &mdash; it never reaches the approval node at all. "
                   "The PHASE row below it is a different layer: <code>headoffice.phase/gate</code> "
                   "refusing a write the governor would have cleared.")
              (table ["Kind" "Rule" "Op" "Unit" "Detail" "Advisor confidence"]
                     (concat (hold-rows ledger) (phase-hold-rows ledger))))

     (section "Action gate"
              (str "Derived from <code>headoffice.phase/phases</code> and "
                   "<code>headoffice.governor/high-stakes</code>. Confidence floor: <code>"
                   (esc governor/confidence-floor) "</code>.")
              (table ["Op" "Writable at phases" "Auto-commit at phases" "Governor stake"]
                     (op-gate-rows)))

     (section "Rollout phases"
              "Derived from <code>headoffice.phase/phases</code>."
              (table ["Phase" "Label" "Writes enabled" "Auto-commit enabled"]
                     (phase-rows)))

     (section "Jurisdiction spec-basis catalog"
              (str "Derived from <code>headoffice.facts/catalog</code>. "
                   "A jurisdiction with no entry has NO spec-basis, and the governor holds any "
                   "proposal that tries to finalize an allocation on it. Coverage over the "
                   "jurisdictions this store actually holds: " (esc (:covered cov)) " of "
                   (esc (:requested cov))
                   (when (seq (:missing-jurisdictions cov))
                     (str " &mdash; missing: "
                          (str/join ", " (map code (:missing-jurisdictions cov))))))
              (table ["ISO3" "Name" "Owner authority" "Legal basis" "Required evidence items" "Units in store"]
                     (jurisdiction-rows db)))

     (section "Approval provenance"
              (str "Measured at render time, not asserted: for each approval this run granted, "
                   "the register the commit actually wrote is inspected for an approver key. "
                   "Where the approver is not in the record, it is shown as audit-only rather "
                   "than omitted &mdash; a reader must be able to tell &ldquo;nobody approved&rdquo; "
                   "from &ldquo;the store did not keep it&rdquo;.")
              (table ["Op" "Unit" "Approver (audit ledger)" "Store register" "Retained in record?"]
                     (provenance-rows db ledger)))

     (section "Allocation-finalization records"
              (str "Drafted by <code>headoffice.registry/register-allocation-finalization</code> "
                   "and appended by the store. Unsigned: signature is the head-office "
                   "operator's own act, not this actor's.")
              (table ["Record" "Unit" "Jurisdiction" "Kind" "Immutability"]
                     (allocation-rows db)))

     (section "Audit ledger (this run)"
              (str "Append-only decision-fact log &mdash; " (esc (count ledger))
                   " facts, every proposal outcome this scenario produced.")
              (table ["#" "Fact" "Op" "Unit" "Disposition" "Basis"]
                     (ledger-rows ledger)))

     "</main>\n"
     "<footer>Generated by <code>headoffice.render-html</code> "
     "(<code>clojure -M:dev:render-html</code>) from a live run of "
     "<code>headoffice.operation</code>. No hand-written rows; no timestamps, so reruns are "
     "byte-identical.</footer>\n"
     "</body></html>\n")))

;; ----------------------------- entry point -----------------------------

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        ledger (vec (store/ledger db))
        hard (hard-holds ledger)
        rules (vec (hard-rules ledger))
        html (render db)]

    ;; Build-time invariant #1: this console exists to show the
    ;; governor refusing things. A run that produced no hold produced
    ;; no evidence, and a page rendered from it would look identical to
    ;; a page whose governor is wired but never fires.
    (when (empty? (holds ledger))
      (throw (ex-info "render-html: the run produced ZERO :governor-hold facts -- refusing to write a console that cannot show a refusal"
                      {:ledger-facts (count ledger)})))
    (when (empty? hard)
      (throw (ex-info "render-html: the run produced no HARD governor hold (holds with violations) -- refusing to write a console whose only refusals are phase-gate holds"
                      {:holds (count (holds ledger))})))

    ;; Build-time invariant #2: the holds must actually reach the page.
    ;; This is what keeps the hold table derived rather than decorative.
    (doseq [r rules]
      (when-not (str/includes? html (name r))
        (throw (ex-info "render-html: a hard rule the governor emitted is missing from the rendered document"
                        {:rule r :rules rules}))))

    ;; Build-time invariant #3: a commit must actually have happened,
    ;; otherwise the clean lifecycle silently degraded into all-holds.
    (when (empty? (store/allocation-history db))
      (throw (ex-info "render-html: no allocation was finalized -- the clean lifecycle did not complete"
                      {:ledger-facts (count ledger)})))

    (spit out html)
    (println "wrote" out
             (str "(" (count ledger) " ledger facts, "
                  (count hard) " HARD holds over " (count rules) " distinct rules: "
                  (str/join ", " (map name rules)) ", "
                  (count (phase-holds ledger)) " phase hold(s), "
                  (count (store/allocation-history db)) " allocation record(s))"))))
