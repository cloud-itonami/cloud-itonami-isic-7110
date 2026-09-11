(ns practiceops.registry-test
  (:require [clojure.test :refer [deftest is]]
            [practiceops.registry :as r]))

;; ----------------------------- fee-total-matches-claim? -----------------------------

(deftest matches-when-claim-equals-recompute
  (is (r/fee-total-matches-claim?
       {:hours 40 :rate 100.0 :claimed-fee 4000.0})))

(deftest mismatches-when-claim-differs-from-recompute
  (is (not (r/fee-total-matches-claim?
            {:hours 30 :rate 120.0 :claimed-fee 4000.0}))))

(deftest compute-fee-total-is-a-flat-hours-times-rate
  (is (= 4000.0 (r/compute-fee-total {:hours 40 :rate 100.0}))))

;; ----------------------------- register-verification -----------------------------

(deftest verification-is-a-draft-not-a-real-verification
  (let [result (r/register-verification "commission-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest verification-assigns-verification-number
  (let [result (r/register-verification "commission-1" "JPN" 7)]
    (is (= (get result "verification_number") "JPN-VER-000007"))
    (is (= (get-in result ["record" "commission_id"]) "commission-1"))
    (is (= (get-in result ["record" "kind"]) "verification-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest verification-validation-rules
  (is (thrown? Exception (r/register-verification "" "JPN" 0)))
  (is (thrown? Exception (r/register-verification "commission-1" "" 0)))
  (is (thrown? Exception (r/register-verification "commission-1" "JPN" -1))))

;; ----------------------------- register-delivery -----------------------------

(deftest delivery-is-a-draft-not-a-real-delivery
  (let [result (r/register-delivery "commission-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest delivery-assigns-delivery-number
  (let [result (r/register-delivery "commission-1" "JPN" 7)]
    (is (= (get result "delivery_number") "JPN-DLV-000007"))
    (is (= (get-in result ["record" "commission_id"]) "commission-1"))
    (is (= (get-in result ["record" "kind"]) "delivery-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest delivery-validation-rules
  (is (thrown? Exception (r/register-delivery "" "JPN" 0)))
  (is (thrown? Exception (r/register-delivery "commission-1" "" 0)))
  (is (thrown? Exception (r/register-delivery "commission-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-verification "commission-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-verification "commission-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-VER-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-VER-000001" (get-in hist2 [1 "record_id"])))))
