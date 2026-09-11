(ns headoffice.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean unit through
  intake -> group-report verification -> allocation-finalization
  proposal (always escalates) -> human approval -> commit, then shows
  four HARD holds (a jurisdiction with no spec-basis, a unit whose own
  recorded transfer price falls outside its own recorded arm's-length
  range, a unit whose own proposed allocation exceeds its own recorded
  authorized limit, and a double finalization of an already-processed
  unit) that never reach a human at all, and prints the audit ledger +
  the draft allocation-finalization records."
  (:require [langgraph.graph :as g]
            [headoffice.store :as store]
            [headoffice.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :head-office-staff :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== unit/intake unit-1 (JPN, clean; transfer price in range, allocation within limit) ==")
    (println (exec! actor "t1" {:op :unit/intake :subject "unit-1"
                                :patch {:id "unit-1" :unit-name "Sato Manufacturing K.K."}} operator))

    (println "== report/verify unit-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :report/verify :subject "unit-1"} operator))
    (println (approve! actor "t2"))

    (println "== actuation/finalize-allocation unit-1 (always escalates -- actuation/finalize-allocation) ==")
    (let [r (exec! actor "t3" {:op :actuation/finalize-allocation :subject "unit-1"} operator)]
      (println r)
      (println "-- human head-office staff approves --")
      (println (approve! actor "t3")))

    (println "== report/verify unit-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t4" {:op :report/verify :subject "unit-2" :no-spec? true} operator))

    (println "== report/verify unit-3 (escalates -- human approves; sets up the transfer-price test) ==")
    (println (exec! actor "t5" {:op :report/verify :subject "unit-3"} operator))
    (println (approve! actor "t5"))

    (println "== actuation/finalize-allocation unit-3 (transfer price 150 outside [90,110] arm's-length range -> HARD hold) ==")
    (println (exec! actor "t6" {:op :actuation/finalize-allocation :subject "unit-3"} operator))

    (println "== report/verify unit-4 (escalates -- human approves; sets up the budget test) ==")
    (println (exec! actor "t7" {:op :report/verify :subject "unit-4"} operator))
    (println (approve! actor "t7"))

    (println "== actuation/finalize-allocation unit-4 (proposed allocation 1500000 exceeds authorized limit 1000000 -> HARD hold) ==")
    (println (exec! actor "t8" {:op :actuation/finalize-allocation :subject "unit-4"} operator))

    (println "== actuation/finalize-allocation unit-1 AGAIN (double-finalization -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/finalize-allocation :subject "unit-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft allocation-finalization records ==")
    (doseq [r (store/allocation-history db)] (println r))))
