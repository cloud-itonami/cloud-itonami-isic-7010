(ns headoffice.registry-test
  (:require [clojure.test :refer [deftest is]]
            [headoffice.registry :as r]))

;; ----------------------------- transfer-price-outside-arms-length-range? -----------------------------

(deftest within-range-is-not-outside
  (is (not (r/transfer-price-outside-arms-length-range?
            {:transfer-price 100 :arms-length-range-min 90 :arms-length-range-max 110})))
  (is (not (r/transfer-price-outside-arms-length-range?
            {:transfer-price 90 :arms-length-range-min 90 :arms-length-range-max 110})))
  (is (not (r/transfer-price-outside-arms-length-range?
            {:transfer-price 110 :arms-length-range-min 90 :arms-length-range-max 110}))))

(deftest above-range-is-outside
  (is (r/transfer-price-outside-arms-length-range?
       {:transfer-price 150 :arms-length-range-min 90 :arms-length-range-max 110})))

(deftest below-range-is-outside
  (is (r/transfer-price-outside-arms-length-range?
       {:transfer-price 50 :arms-length-range-min 90 :arms-length-range-max 110})))

(deftest missing-fields-are-not-treated-as-outside
  (is (not (r/transfer-price-outside-arms-length-range? {})))
  (is (not (r/transfer-price-outside-arms-length-range? {:transfer-price 150}))))

;; ----------------------------- budget-allocation-exceeds-authorized-limit? -----------------------------

(deftest not-exceeding-when-within-limit
  (is (not (r/budget-allocation-exceeds-authorized-limit?
            {:proposed-allocation-amount 500000 :authorized-allocation-limit 1000000})))
  (is (not (r/budget-allocation-exceeds-authorized-limit?
            {:proposed-allocation-amount 1000000 :authorized-allocation-limit 1000000}))))

(deftest exceeding-when-over-limit
  (is (r/budget-allocation-exceeds-authorized-limit?
       {:proposed-allocation-amount 1500000 :authorized-allocation-limit 1000000})))

(deftest missing-fields-are-not-treated-as-exceeding
  (is (not (r/budget-allocation-exceeds-authorized-limit? {})))
  (is (not (r/budget-allocation-exceeds-authorized-limit? {:proposed-allocation-amount 1500000}))))

;; ----------------------------- register-allocation-finalization -----------------------------

(deftest finalization-is-a-draft-not-a-real-finalization
  (let [result (r/register-allocation-finalization "unit-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest finalization-assigns-allocation-number
  (let [result (r/register-allocation-finalization "unit-1" "JPN" 7)]
    (is (= (get result "allocation_number") "JPN-ALC-000007"))
    (is (= (get-in result ["record" "unit_id"]) "unit-1"))
    (is (= (get-in result ["record" "kind"]) "allocation-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest finalization-validation-rules
  (is (thrown? Exception (r/register-allocation-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-allocation-finalization "unit-1" "" 0)))
  (is (thrown? Exception (r/register-allocation-finalization "unit-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-allocation-finalization "unit-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-allocation-finalization "unit-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-ALC-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-ALC-000001" (get-in hist2 [1 "record_id"])))))
