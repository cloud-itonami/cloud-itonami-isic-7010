(ns headoffice.store
  "SSoT for the head-office actor, behind a `Store` protocol so the
  backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/headoffice/store_contract_test.clj), which is the whole
  point: the actor, the Group Oversight Governor and the audit
  ledger never know which SSoT they run on.

  Like `clinic.store`'s/`personalservice.store`'s simpler entities, a
  UNIT is acted on directly by the ONE actuation op -- no dynamically-
  filed sub-record, and the double-finalization guard checks a
  dedicated `:allocation-finalized?` boolean rather than a `:status`
  value, the same discipline `clinic.governor`'s/`personalservice.
  governor`'s guards establish.

  NOTE on naming: the protocol's per-entity accessor is `unit`
  directly -- not a Clojure special form, so no `-of` suffix
  workaround was needed.

  The ledger stays append-only on every backend: 'which unit's
  transfer price was checked against its own arm's-length range,
  which allocation was finalized, on what jurisdictional basis,
  approved by whom' is always a query over an immutable log -- the
  audit trail a group entity trusting a head-office operator needs,
  and the evidence an operator needs if an allocation decision is
  later disputed."
  (:require [headoffice.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (unit [s id])
  (all-units [s])
  (report-of [s unit-id] "committed group-report evidence checklist, or nil")
  (ledger [s])
  (allocation-history [s] "the append-only allocation-finalization history (headoffice.registry drafts)")
  (next-sequence [s jurisdiction] "next allocation-number sequence for a jurisdiction")
  (unit-already-finalized? [s unit-id] "has this unit's allocation already been finalized?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-units [s units] "replace/seed the unit directory (map id->unit)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained unit set so the actor + tests run
  offline."
  []
  {:units
   {"unit-1" {:id "unit-1" :unit-name "Sato Manufacturing K.K."
             :proposed-allocation-amount 500000 :authorized-allocation-limit 1000000
             :transfer-price 100 :arms-length-range-min 90 :arms-length-range-max 110
             :allocation-finalized? false :jurisdiction "JPN" :status :intake}
    "unit-2" {:id "unit-2" :unit-name "Atlantis Holdings"
             :proposed-allocation-amount 500000 :authorized-allocation-limit 1000000
             :transfer-price 100 :arms-length-range-min 90 :arms-length-range-max 110
             :allocation-finalized? false :jurisdiction "ATL" :status :intake}
    "unit-3" {:id "unit-3" :unit-name "鈴木物流株式会社"
             :proposed-allocation-amount 500000 :authorized-allocation-limit 1000000
             :transfer-price 150 :arms-length-range-min 90 :arms-length-range-max 110
             :allocation-finalized? false :jurisdiction "JPN" :status :intake}
    "unit-4" {:id "unit-4" :unit-name "田中商事株式会社"
             :proposed-allocation-amount 1500000 :authorized-allocation-limit 1000000
             :transfer-price 100 :arms-length-range-min 90 :arms-length-range-max 110
             :allocation-finalized? false :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- finalize-allocation!
  "Backend-agnostic `:unit/mark-finalized` -- looks up the unit via the
  protocol and drafts the allocation-finalization record, and returns
  {:result .. :unit-patch ..} for the caller to persist."
  [s unit-id]
  (let [u (unit s unit-id)
        seq-n (next-sequence s (:jurisdiction u))
        result (registry/register-allocation-finalization unit-id (:jurisdiction u) seq-n)]
    {:result result
     :unit-patch {:allocation-finalized? true
                 :allocation-number (get result "allocation_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (unit [_ id] (get-in @a [:units id]))
  (all-units [_] (sort-by :id (vals (:units @a))))
  (report-of [_ unit-id] (get-in @a [:reports unit-id]))
  (ledger [_] (:ledger @a))
  (allocation-history [_] (:allocations @a))
  (next-sequence [_ jurisdiction] (get-in @a [:sequences jurisdiction] 0))
  (unit-already-finalized? [_ unit-id] (boolean (get-in @a [:units unit-id :allocation-finalized?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :unit/upsert
      (swap! a update-in [:units (:id value)] merge value)

      :report/set
      (swap! a assoc-in [:reports (first path)] payload)

      :unit/mark-finalized
      (let [unit-id (first path)
            {:keys [result unit-patch]} (finalize-allocation! s unit-id)
            jurisdiction (:jurisdiction (unit s unit-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:sequences jurisdiction] (fnil inc 0))
                       (update-in [:units unit-id] merge unit-patch)
                       (update :allocations registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-units [s units] (when (seq units) (swap! a assoc :units units)) s))

(defn seed-db
  "A MemStore seeded with the demo unit set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :reports {} :ledger [] :sequences {}
                           :allocations []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (report payloads, ledger facts, allocation records)
  are stored as EDN strings so `langchain.db` doesn't expand them into
  sub-entities -- the same convention every sibling actor's store
  uses."
  {:unit/id                {:db/unique :db.unique/identity}
   :report/unit-id          {:db/unique :db.unique/identity}
   :ledger/seq               {:db/unique :db.unique/identity}
   :allocation/seq             {:db/unique :db.unique/identity}
   :sequence/jurisdiction         {:db/unique :db.unique/identity}})

(defn- enc [v] (ls/enc v))
(defn- dec* [s] (ls/dec* s))

(defn- unit->tx [{:keys [id unit-name proposed-allocation-amount authorized-allocation-limit
                        transfer-price arms-length-range-min arms-length-range-max
                        allocation-finalized? jurisdiction status allocation-number]}]
  (cond-> {:unit/id id}
    unit-name                                 (assoc :unit/unit-name unit-name)
    proposed-allocation-amount                 (assoc :unit/proposed-allocation-amount proposed-allocation-amount)
    authorized-allocation-limit                  (assoc :unit/authorized-allocation-limit authorized-allocation-limit)
    transfer-price                                (assoc :unit/transfer-price transfer-price)
    arms-length-range-min                          (assoc :unit/arms-length-range-min arms-length-range-min)
    arms-length-range-max                           (assoc :unit/arms-length-range-max arms-length-range-max)
    (some? allocation-finalized?)                    (assoc :unit/allocation-finalized? allocation-finalized?)
    jurisdiction                                       (assoc :unit/jurisdiction jurisdiction)
    status                                              (assoc :unit/status status)
    allocation-number                                    (assoc :unit/allocation-number allocation-number)))

(def ^:private unit-pull
  [:unit/id :unit/unit-name :unit/proposed-allocation-amount :unit/authorized-allocation-limit
   :unit/transfer-price :unit/arms-length-range-min :unit/arms-length-range-max
   :unit/allocation-finalized? :unit/jurisdiction :unit/status :unit/allocation-number])

(defn- pull->unit [m]
  (when (:unit/id m)
    {:id (:unit/id m) :unit-name (:unit/unit-name m)
     :proposed-allocation-amount (:unit/proposed-allocation-amount m)
     :authorized-allocation-limit (:unit/authorized-allocation-limit m)
     :transfer-price (:unit/transfer-price m)
     :arms-length-range-min (:unit/arms-length-range-min m)
     :arms-length-range-max (:unit/arms-length-range-max m)
     :allocation-finalized? (boolean (:unit/allocation-finalized? m))
     :jurisdiction (:unit/jurisdiction m) :status (:unit/status m)
     :allocation-number (:unit/allocation-number m)}))

(defrecord DatomicStore [conn]
  Store
  (unit [_ id]
    (pull->unit (d/pull (d/db conn) unit-pull [:unit/id id])))
  (all-units [_]
    (->> (d/q '[:find [?id ...] :where [?e :unit/id ?id]] (d/db conn))
         (map #(pull->unit (d/pull (d/db conn) unit-pull [:unit/id %])))
         (sort-by :id)))
  (report-of [_ unit-id]
    (dec* (d/q '[:find ?p . :in $ ?uid
                :where [?a :report/unit-id ?uid] [?a :report/payload ?p]]
              (d/db conn) unit-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (allocation-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :allocation/seq ?s] [?e :allocation/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :sequence/jurisdiction ?j] [?e :sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (unit-already-finalized? [s unit-id]
    (boolean (:allocation-finalized? (unit s unit-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :unit/upsert
      (d/transact! conn [(unit->tx value)])

      :report/set
      (d/transact! conn [{:report/unit-id (first path) :report/payload (enc payload)}])

      :unit/mark-finalized
      (let [unit-id (first path)
            {:keys [result unit-patch]} (finalize-allocation! s unit-id)
            jurisdiction (:jurisdiction (unit s unit-id))
            next-n (inc (next-sequence s jurisdiction))]
        (d/transact! conn
                     [(unit->tx (assoc unit-patch :id unit-id))
                      {:sequence/jurisdiction jurisdiction :sequence/next next-n}
                      {:allocation/seq (count (allocation-history s)) :allocation/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-units [s units]
    (when (seq units) (d/transact! conn (mapv unit->tx (vals units)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:units ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [units]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-units s units))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo unit set -- the Datomic-backed
  analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
