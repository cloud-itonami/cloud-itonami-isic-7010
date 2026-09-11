(ns headoffice.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:actuation/finalize-allocation` must NEVER be a member
  of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [headoffice.phase :as phase]))

(deftest finalize-allocation-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real allocation finalization"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/finalize-allocation))
          (str "phase " n " must not auto-commit :actuation/finalize-allocation")))))

(deftest report-verify-never-auto-at-any-phase
  (testing "verification carries no direct capital risk, but is still never auto-eligible, matching every sibling verification op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :report/verify))
          (str "phase " n " must not auto-commit :report/verify")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":unit/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:unit/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :unit/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/finalize-allocation} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :unit/intake} :commit)))))
