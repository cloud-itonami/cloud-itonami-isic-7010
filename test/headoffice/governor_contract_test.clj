(ns headoffice.governor-contract-test
  "The governor contract as executable tests -- the head-office analog
  of `cloud-itonami-isic-6512`'s `casualty.governor-contract-test`.
  The single invariant under test:

    HeadOffice-LLM never finalizes an allocation the Group Oversight
    Governor would reject, `:actuation/finalize-allocation` NEVER
    auto-commits at any phase, `:unit/intake` (no direct capital risk)
    MAY auto-commit when clean, and every decision (commit OR hold)
    leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [headoffice.store :as store]
            [headoffice.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :head-office-staff :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a group-report
  evidence checklist on file. Uses distinct thread-ids per call site
  by suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :report/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :unit/intake :subject "unit-1"
                   :patch {:id "unit-1" :unit-name "Sato Manufacturing K.K."}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sato Manufacturing K.K." (:unit-name (store/unit db "unit-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest report-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :report/verify :subject "unit-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/report-of db "unit-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a report/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :report/verify :subject "unit-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/report-of db "unit-1")) "no report written"))))

(deftest finalize-allocation-without-report-is-held
  (testing "actuation/finalize-allocation before any group-report verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/finalize-allocation :subject "unit-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest transfer-price-outside-arms-length-range-is-held
  (testing "a unit whose own recorded transfer price falls outside its own recorded arm's-length range -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "unit-3")
          res (exec-op actor "t5" {:op :actuation/finalize-allocation :subject "unit-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:transfer-price-outside-arms-length-range} (-> (store/ledger db) last :basis)))
      (is (empty? (store/allocation-history db))))))

(deftest budget-allocation-exceeds-authorized-limit-is-held
  (testing "a unit whose own proposed allocation exceeds its own recorded authorized limit -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t6pre" "unit-4")
          res (exec-op actor "t6" {:op :actuation/finalize-allocation :subject "unit-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:budget-allocation-exceeds-authorized-limit} (-> (store/ledger db) last :basis)))
      (is (empty? (store/allocation-history db))))))

(deftest finalize-allocation-always-escalates-then-human-decides
  (testing "a clean, fully-reported unit still ALWAYS interrupts for human approval -- actuation/finalize-allocation is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "unit-1")
          r1 (exec-op actor "t7" {:op :actuation/finalize-allocation :subject "unit-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, allocation-finalization record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:allocation-finalized? (store/unit db "unit-1"))))
          (is (= 1 (count (store/allocation-history db))) "one draft finalization record"))))))

(deftest double-finalization-is-held
  (testing "finalizing the same unit's allocation twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "unit-1")
          _ (exec-op actor "t8a" {:op :actuation/finalize-allocation :subject "unit-1"} operator)
          _ (approve! actor "t8a")
          res (exec-op actor "t8" {:op :actuation/finalize-allocation :subject "unit-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-finalized} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/allocation-history db))) "still only the one earlier finalization"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :unit/intake :subject "unit-1"
                          :patch {:id "unit-1" :unit-name "Sato Manufacturing K.K."}} operator)
      (exec-op actor "b" {:op :report/verify :subject "unit-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
