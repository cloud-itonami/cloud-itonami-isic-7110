(ns practiceops.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [practiceops.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "JPN" (:jurisdiction (store/commission s "commission-1"))))
      (is (= 4000.0 (:claimed-fee (store/commission s "commission-1"))))
      (is (true? (:within-code-scope? (store/commission s "commission-1"))))
      (is (false? (:requires-professional-seal? (store/commission s "commission-1"))))
      (is (= 4000.0 (:claimed-fee (store/commission s "commission-3"))))
      (is (false? (:within-code-scope? (store/commission s "commission-4"))))
      (is (true? (:requires-professional-seal? (store/commission s "commission-5"))))
      (is (false? (:seal-authority-verified? (store/commission s "commission-5"))))
      (is (true? (:seal-authority-verified? (store/commission s "commission-6"))))
      (is (false? (:verified? (store/commission s "commission-1"))))
      (is (false? (:delivered? (store/commission s "commission-1"))))
      (is (= ["commission-1" "commission-2" "commission-3" "commission-4" "commission-5" "commission-6"]
             (mapv :id (store/all-commissions s))))
      (is (nil? (store/assessment-of s "commission-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/verification-history s)))
      (is (= [] (store/delivery-history s)))
      (is (zero? (store/next-verification-sequence s "JPN")))
      (is (zero? (store/next-delivery-sequence s "JPN")))
      (is (false? (store/commission-already-verified? s "commission-1")))
      (is (false? (store/commission-already-delivered? s "commission-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :commission/upsert
                                 :value {:id "commission-1" :client "Kita Cooperative"}})
        (is (= "Kita Cooperative" (:client (store/commission s "commission-1"))))
        (is (= 4000.0 (:claimed-fee (store/commission s "commission-1"))) "unrelated field preserved"))
      (testing "assessment payloads commit and read back"
        (store/commit-record! s {:effect :assessment/set :path ["commission-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/assessment-of s "commission-1"))))
      (testing "verification drafts a record and advances the verification sequence"
        (store/commit-record! s {:effect :commission/mark-verified :path ["commission-1"]})
        (is (= "JPN-VER-000000" (get (first (store/verification-history s)) "record_id")))
        (is (= "verification-draft" (get (first (store/verification-history s)) "kind")))
        (is (true? (:verified? (store/commission s "commission-1"))))
        (is (= 1 (count (store/verification-history s))))
        (is (= 1 (store/next-verification-sequence s "JPN")))
        (is (true? (store/commission-already-verified? s "commission-1"))))
      (testing "delivery drafts a record and advances the delivery sequence"
        (store/commit-record! s {:effect :commission/mark-delivered :path ["commission-1"]})
        (is (= "JPN-DLV-000000" (get (first (store/delivery-history s)) "record_id")))
        (is (= "delivery-draft" (get (first (store/delivery-history s)) "kind")))
        (is (true? (:delivered? (store/commission s "commission-1"))))
        (is (= 1 (count (store/delivery-history s))))
        (is (= 1 (store/next-delivery-sequence s "JPN")))
        (is (true? (store/commission-already-delivered? s "commission-1"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/commission s "nope")))
    (is (= [] (store/all-commissions s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/verification-history s)))
    (is (= [] (store/delivery-history s)))
    (is (zero? (store/next-verification-sequence s "JPN")))
    (is (zero? (store/next-delivery-sequence s "JPN")))
    (store/with-commissions s {"x" {:id "x" :client "c" :project-type :school-addition
                                    :hours 1 :rate 1.0 :claimed-fee 1.0
                                    :within-code-scope? true
                                    :requires-professional-seal? false :seal-authority-verified? false
                                    :verified? false :delivered? false
                                    :jurisdiction "JPN" :status :intake}})
    (is (= "c" (:client (store/commission s "x"))))))
