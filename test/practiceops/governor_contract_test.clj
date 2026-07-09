(ns practiceops.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls ('design outside code/scope is blocked; verification
  evidence is required; certification is auditable') implemented
  faithfully. The single invariant under test:

    PracticeOps-LLM never verifies a design or delivers a
    certification the Design Governor would reject, `:design/verify`/
    `:design/deliver` NEVER auto-commit at any phase, `:commission/
    intake` (no direct professional-liability risk) MAY auto-commit
    when clean, and every decision (commit OR hold) leaves exactly one
    ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [practiceops.store :as store]
            [practiceops.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :practice-principal :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  "Walks `subject` through assess -> approve, leaving an assessment on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving :verified? true.
  Assumes `assess!` already ran for this subject."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :design/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :commission/intake :subject "commission-1"
                   :patch {:id "commission-1" :client "Kita Cooperative"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Kita Cooperative" (:client (store/commission db "commission-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "commission-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "commission-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "commission-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "commission-1")) "no assessment written"))))

(deftest verify-without-assessment-is-held
  (testing "design/verify before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :design/verify :subject "commission-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest design-outside-scope-is-held-and-unoverridable
  (testing "a design outside code/scope -> HOLD, and never reaches request-approval -- the FLAGSHIP genuinely new check this vertical adds, the 82nd unconditional-evaluation-discipline grounding overall, grounded in Japan's own 建築基準法, the US's IBC (as locally adopted), the UK's Building Regulations 2010 and Germany's Musterbauordnung"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "commission-4")
          res (exec-op actor "t5" {:op :design/verify :subject "commission-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:design-outside-scope} (-> (store/ledger db) last :basis)))
      (is (empty? (store/verification-history db))))))

(deftest fee-total-mismatch-is-held
  (testing "a claimed fee that doesn't equal hours x rate -> HOLD (the ground-truth-recompute discipline every sibling's cost/total-matching check establishes)"
    (let [[db actor] (fresh)
          _ (assess! actor "t6pre" "commission-3")
          _ (verify! actor "t6pre" "commission-3")
          res (exec-op actor "t6" {:op :design/deliver :subject "commission-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:fee-total-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest professional-seal-invalid-is-held-and-unoverridable
  (testing "an unverified professional seal on a seal-required commission -> HOLD, and never reaches request-approval -- a genuinely new check, the 83rd unconditional-evaluation-discipline grounding overall, the ELEVENTH conditional variant (see this actor's governor ns docstring / the full accumulated ADR-0001 chain: parksafety's ADR-2607071922 Decision 5 through leathergoods's, ictrepair's, retailops's, freightops's, quarryops's, agronomyops's and hospitalityops's own)"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "commission-5")
          _ (verify! actor "t7pre" "commission-5")
          res (exec-op actor "t7" {:op :design/deliver :subject "commission-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:professional-seal-invalid} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest deliver-is-a-noop-when-no-seal-required
  (testing "the professional-seal check is CONDITIONAL: a commission with no seal requirement has no seal-authority requirement at all"
    (let [[_db actor] (fresh)
          _ (assess! actor "t7bpre" "commission-1")
          _ (verify! actor "t7bpre" "commission-1")
          res (exec-op actor "t7b" {:op :design/deliver :subject "commission-1"} operator)]
      (is (= :interrupted (:status res)) "clean delivery still escalates for human sign-off, but is NOT a HARD hold"))))

(deftest deliver-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, matching-fee, no-seal-required delivery still ALWAYS interrupts for human approval -- actuation/deliver-certification is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "commission-1")
          _ (verify! actor "t8pre" "commission-1")
          r1 (exec-op actor "t8" {:op :design/deliver :subject "commission-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, delivery record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:delivered? (store/commission db "commission-1"))))
          (is (= 1 (count (store/delivery-history db))) "one draft delivery record"))))))

(deftest verify-always-escalates-then-human-decides
  (testing "a clean, fully-assessed verification still ALWAYS interrupts for human approval -- actuation/verify-design is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "commission-1")
          r1 (exec-op actor "t9" {:op :design/verify :subject "commission-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, verification record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:verified? (store/commission db "commission-1"))))
          (is (= 1 (count (store/verification-history db))) "one draft verification record"))))))

(deftest commission-double-verification-is-held
  (testing "verifying the same commission record twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "commission-1")
          _ (verify! actor "t10pre" "commission-1")
          res (exec-op actor "t10" {:op :design/verify :subject "commission-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-verified} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/verification-history db))) "still only the one earlier verification"))))

(deftest commission-double-delivery-is-held
  (testing "delivering the same commission twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "commission-1")
          _ (verify! actor "t11pre" "commission-1")
          _ (exec-op actor "t11a" {:op :design/deliver :subject "commission-1"} operator)
          _ (approve! actor "t11a")
          res (exec-op actor "t11" {:op :design/deliver :subject "commission-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-delivered} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/delivery-history db))) "still only the one earlier delivery"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :commission/intake :subject "commission-1"
                          :patch {:id "commission-1" :client "Kita Cooperative"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "commission-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
