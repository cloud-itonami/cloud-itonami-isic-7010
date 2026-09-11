(ns headoffice.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [headoffice.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Manufacturing K.K." (:unit-name (store/unit s "unit-1"))))
      (is (= "JPN" (:jurisdiction (store/unit s "unit-1"))))
      (is (= 100 (:transfer-price (store/unit s "unit-1"))))
      (is (= 500000 (:proposed-allocation-amount (store/unit s "unit-1"))))
      (is (= 150 (:transfer-price (store/unit s "unit-3"))))
      (is (= 1500000 (:proposed-allocation-amount (store/unit s "unit-4"))))
      (is (false? (:allocation-finalized? (store/unit s "unit-1"))))
      (is (= ["unit-1" "unit-2" "unit-3" "unit-4"]
             (mapv :id (store/all-units s))))
      (is (nil? (store/report-of s "unit-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/allocation-history s)))
      (is (zero? (store/next-sequence s "JPN")))
      (is (false? (store/unit-already-finalized? s "unit-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :unit/upsert
                                 :value {:id "unit-1" :unit-name "Sato Manufacturing K.K."}})
        (is (= "Sato Manufacturing K.K." (:unit-name (store/unit s "unit-1"))))
        (is (= 100 (:transfer-price (store/unit s "unit-1"))) "unrelated field preserved"))
      (testing "report payloads commit and read back"
        (store/commit-record! s {:effect :report/set :path ["unit-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/report-of s "unit-1"))))
      (testing "allocation finalization drafts a record and advances the sequence"
        (store/commit-record! s {:effect :unit/mark-finalized :path ["unit-1"]})
        (is (= "JPN-ALC-000000" (get (first (store/allocation-history s)) "record_id")))
        (is (= "allocation-finalization-draft" (get (first (store/allocation-history s)) "kind")))
        (is (true? (:allocation-finalized? (store/unit s "unit-1"))))
        (is (= 1 (count (store/allocation-history s))))
        (is (= 1 (store/next-sequence s "JPN")))
        (is (true? (store/unit-already-finalized? s "unit-1")))
        (is (false? (store/unit-already-finalized? s "unit-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/unit s "nope")))
    (is (= [] (store/all-units s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/allocation-history s)))
    (is (zero? (store/next-sequence s "JPN")))
    (store/with-units s {"x" {:id "x" :unit-name "n"
                              :proposed-allocation-amount 100 :authorized-allocation-limit 200
                              :transfer-price 100 :arms-length-range-min 90 :arms-length-range-max 110
                              :allocation-finalized? false :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:unit-name (store/unit s "x"))))))
