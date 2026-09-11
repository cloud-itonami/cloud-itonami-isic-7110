(ns practiceops.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:design/verify`/`:design/deliver` must NEVER be a
  member of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [practiceops.phase :as phase]))

(deftest design-verify-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real design verification"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :design/verify))
          (str "phase " n " must not auto-commit :design/verify")))))

(deftest design-deliver-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real certification delivery"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :design/deliver))
          (str "phase " n " must not auto-commit :design/deliver")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-liability-risk-ops
  (testing ":commission/intake carries no direct professional-liability risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:commission/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :commission/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :design/verify} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :design/deliver} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :commission/intake} :commit)))))
