(ns practiceops.store
  "SSoT for the community-architectural/engineering-practice actor,
  behind a `Store` protocol so the backend is a swap, not a rewrite --
  the same seam every prior `cloud-itonami-isic-*` actor in this fleet
  uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/practiceops/store_contract_test.clj), which is the whole
  point: the actor, the Design Governor and the audit ledger never
  know which SSoT they run on.

  Like `hospitalityops`/5510's own `stay`, the primary entity here is
  a `commission` -- design-verification and certification-delivery
  actuation events apply SEQUENTIALLY to the SAME commission record
  (verify first, deliver later), matching the freight/quarry/agronomy/
  hospitality cluster's own sequential entity shape. Dedicated
  double-actuation-guard booleans (`:verified?`/`:delivered?`, never a
  `:status` value).

  The ledger stays append-only on every backend: 'which commission was
  screened for a design outside code/scope or an invalid professional
  seal, which design was verified, which certification was delivered,
  on what jurisdictional basis, approved by whom' is always a query
  over an immutable log -- the audit trail a small practice or public-
  works program trusting an architectural/engineering operator needs,
  and the evidence an operator needs if a verification or a delivery
  is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [practiceops.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (commission [s id])
  (all-commissions [s])
  (assessment-of [s commission-id] "committed jurisdiction assessment, or nil")
  (ledger [s])
  (verification-history [s] "the append-only design-verification history (practiceops.registry drafts)")
  (delivery-history [s] "the append-only certification-delivery history (practiceops.registry drafts)")
  (next-verification-sequence [s jurisdiction] "next verification-number sequence for a jurisdiction")
  (next-delivery-sequence [s jurisdiction] "next delivery-number sequence for a jurisdiction")
  (commission-already-verified? [s commission-id] "has this commission's design already been verified?")
  (commission-already-delivered? [s commission-id] "has this commission's certification already been delivered?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-commissions [s commissions] "replace/seed the commission directory (map id->commission)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained commission set covering both actuation
  lifecycles (verify, deliver) plus the governor's own new checks, so
  the actor + tests run offline."
  []
  {:commissions
   {"commission-1" {:id "commission-1" :client "Kita Cooperative" :project-type :school-addition
                     :hours 40 :rate 100.0 :claimed-fee 4000.0
                     :within-code-scope? true
                     :requires-professional-seal? false :seal-authority-verified? false
                     :verified? false :delivered? false
                     :jurisdiction "JPN" :status :intake}
    "commission-2" {:id "commission-2" :client "Atlantis Cooperative" :project-type :school-addition
                     :hours 20 :rate 100.0 :claimed-fee 2000.0
                     :within-code-scope? true
                     :requires-professional-seal? false :seal-authority-verified? false
                     :verified? false :delivered? false
                     :jurisdiction "ATL" :status :intake}
    "commission-3" {:id "commission-3" :client "Minami Cooperative" :project-type :footbridge
                     :hours 30 :rate 120.0 :claimed-fee 4000.0
                     :within-code-scope? true
                     :requires-professional-seal? false :seal-authority-verified? false
                     :verified? false :delivered? false
                     :jurisdiction "JPN" :status :intake}
    "commission-4" {:id "commission-4" :client "Higashi Cooperative" :project-type :high-rise-addition
                     :hours 50 :rate 110.0 :claimed-fee 5500.0
                     :within-code-scope? false
                     :requires-professional-seal? false :seal-authority-verified? false
                     :verified? false :delivered? false
                     :jurisdiction "JPN" :status :intake}
    "commission-5" {:id "commission-5" :client "Nishi Cooperative" :project-type :public-works-bridge
                     :hours 60 :rate 100.0 :claimed-fee 6000.0
                     :within-code-scope? true
                     :requires-professional-seal? true :seal-authority-verified? false
                     :verified? false :delivered? false
                     :jurisdiction "JPN" :status :intake}
    "commission-6" {:id "commission-6" :client "Chuo Cooperative" :project-type :public-works-bridge
                     :hours 70 :rate 100.0 :claimed-fee 7000.0
                     :within-code-scope? true
                     :requires-professional-seal? true :seal-authority-verified? true
                     :verified? false :delivered? false
                     :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- verify-design!
  "Backend-agnostic `:commission/mark-verified` -- looks up the
  commission via the protocol and drafts the verification record, and
  returns {:result .. :commission-patch ..} for the caller to
  persist."
  [s commission-id]
  (let [c (commission s commission-id)
        seq-n (next-verification-sequence s (:jurisdiction c))
        result (registry/register-verification commission-id (:jurisdiction c) seq-n)]
    {:result result
     :commission-patch {:verified? true
                        :verification-number (get result "verification_number")}}))

(defn- deliver-certification!
  "Backend-agnostic `:commission/mark-delivered` -- looks up the
  commission via the protocol and drafts the delivery record, and
  returns {:result .. :commission-patch ..} for the caller to
  persist."
  [s commission-id]
  (let [c (commission s commission-id)
        seq-n (next-delivery-sequence s (:jurisdiction c))
        result (registry/register-delivery commission-id (:jurisdiction c) seq-n)]
    {:result result
     :commission-patch {:delivered? true
                        :delivery-number (get result "delivery_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (commission [_ id] (get-in @a [:commissions id]))
  (all-commissions [_] (sort-by :id (vals (:commissions @a))))
  (assessment-of [_ commission-id] (get-in @a [:assessments commission-id]))
  (ledger [_] (:ledger @a))
  (verification-history [_] (:verification-records @a))
  (delivery-history [_] (:delivery-records @a))
  (next-verification-sequence [_ jurisdiction] (get-in @a [:verification-sequences jurisdiction] 0))
  (next-delivery-sequence [_ jurisdiction] (get-in @a [:delivery-sequences jurisdiction] 0))
  (commission-already-verified? [_ commission-id] (boolean (get-in @a [:commissions commission-id :verified?])))
  (commission-already-delivered? [_ commission-id] (boolean (get-in @a [:commissions commission-id :delivered?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :commission/upsert
      (swap! a update-in [:commissions (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :commission/mark-verified
      (let [commission-id (first path)
            {:keys [result commission-patch]} (verify-design! s commission-id)
            jurisdiction (:jurisdiction (commission s commission-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:verification-sequences jurisdiction] (fnil inc 0))
                       (update-in [:commissions commission-id] merge commission-patch)
                       (update :verification-records registry/append result))))
        result)

      :commission/mark-delivered
      (let [commission-id (first path)
            {:keys [result commission-patch]} (deliver-certification! s commission-id)
            jurisdiction (:jurisdiction (commission s commission-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:delivery-sequences jurisdiction] (fnil inc 0))
                       (update-in [:commissions commission-id] merge commission-patch)
                       (update :delivery-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-commissions [s commissions] (when (seq commissions) (swap! a assoc :commissions commissions)) s))

(defn seed-db
  "A MemStore seeded with the demo commission set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :verification-sequences {} :verification-records []
                           :delivery-sequences {} :delivery-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (assessment payloads, ledger facts,
  verification/delivery records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses."
  {:commission/id                  {:db/unique :db.unique/identity}
   :assessment/commission-id       {:db/unique :db.unique/identity}
   :ledger/seq                     {:db/unique :db.unique/identity}
   :verification-record/seq        {:db/unique :db.unique/identity}
   :delivery-record/seq            {:db/unique :db.unique/identity}
   :verification-sequence/jurisdiction {:db/unique :db.unique/identity}
   :delivery-sequence/jurisdiction     {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- commission->tx [{:keys [id client project-type hours rate claimed-fee
                               within-code-scope?
                               requires-professional-seal? seal-authority-verified?
                               verified? delivered?
                               jurisdiction status verification-number delivery-number]}]
  (cond-> {:commission/id id}
    client                                          (assoc :commission/client client)
    project-type                                       (assoc :commission/project-type project-type)
    hours                                                 (assoc :commission/hours hours)
    rate                                                     (assoc :commission/rate rate)
    claimed-fee                                                 (assoc :commission/claimed-fee claimed-fee)
    (some? within-code-scope?)                                     (assoc :commission/within-code-scope? within-code-scope?)
    (some? requires-professional-seal?)                               (assoc :commission/requires-professional-seal? requires-professional-seal?)
    (some? seal-authority-verified?)                                     (assoc :commission/seal-authority-verified? seal-authority-verified?)
    (some? verified?)                                                       (assoc :commission/verified? verified?)
    (some? delivered?)                                                         (assoc :commission/delivered? delivered?)
    jurisdiction                                                                   (assoc :commission/jurisdiction jurisdiction)
    status                                                                            (assoc :commission/status status)
    verification-number                                                                (assoc :commission/verification-number verification-number)
    delivery-number                                                                        (assoc :commission/delivery-number delivery-number)))

(def ^:private commission-pull
  [:commission/id :commission/client :commission/project-type :commission/hours :commission/rate :commission/claimed-fee
   :commission/within-code-scope? :commission/requires-professional-seal? :commission/seal-authority-verified?
   :commission/verified? :commission/delivered?
   :commission/jurisdiction :commission/status :commission/verification-number :commission/delivery-number])

(defn- pull->commission [m]
  (when (:commission/id m)
    {:id (:commission/id m) :client (:commission/client m) :project-type (:commission/project-type m)
     :hours (:commission/hours m) :rate (:commission/rate m) :claimed-fee (:commission/claimed-fee m)
     :within-code-scope? (boolean (:commission/within-code-scope? m))
     :requires-professional-seal? (boolean (:commission/requires-professional-seal? m))
     :seal-authority-verified? (boolean (:commission/seal-authority-verified? m))
     :verified? (boolean (:commission/verified? m)) :delivered? (boolean (:commission/delivered? m))
     :jurisdiction (:commission/jurisdiction m) :status (:commission/status m)
     :verification-number (:commission/verification-number m) :delivery-number (:commission/delivery-number m)}))

(defrecord DatomicStore [conn]
  Store
  (commission [_ id]
    (pull->commission (d/pull (d/db conn) commission-pull [:commission/id id])))
  (all-commissions [_]
    (->> (d/q '[:find [?id ...] :where [?e :commission/id ?id]] (d/db conn))
         (map #(pull->commission (d/pull (d/db conn) commission-pull [:commission/id %])))
         (sort-by :id)))
  (assessment-of [_ commission-id]
    (dec* (d/q '[:find ?p . :in $ ?cid
                :where [?a :assessment/commission-id ?cid] [?a :assessment/payload ?p]]
              (d/db conn) commission-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (verification-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :verification-record/seq ?s] [?e :verification-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (delivery-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :delivery-record/seq ?s] [?e :delivery-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-verification-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :verification-sequence/jurisdiction ?j] [?e :verification-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-delivery-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :delivery-sequence/jurisdiction ?j] [?e :delivery-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (commission-already-verified? [s commission-id]
    (boolean (:verified? (commission s commission-id))))
  (commission-already-delivered? [s commission-id]
    (boolean (:delivered? (commission s commission-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :commission/upsert
      (d/transact! conn [(commission->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/commission-id (first path) :assessment/payload (enc payload)}])

      :commission/mark-verified
      (let [commission-id (first path)
            {:keys [result commission-patch]} (verify-design! s commission-id)
            jurisdiction (:jurisdiction (commission s commission-id))
            next-n (inc (next-verification-sequence s jurisdiction))]
        (d/transact! conn
                     [(commission->tx (assoc commission-patch :id commission-id))
                      {:verification-sequence/jurisdiction jurisdiction :verification-sequence/next next-n}
                      {:verification-record/seq (count (verification-history s)) :verification-record/record (enc (get result "record"))}])
        result)

      :commission/mark-delivered
      (let [commission-id (first path)
            {:keys [result commission-patch]} (deliver-certification! s commission-id)
            jurisdiction (:jurisdiction (commission s commission-id))
            next-n (inc (next-delivery-sequence s jurisdiction))]
        (d/transact! conn
                     [(commission->tx (assoc commission-patch :id commission-id))
                      {:delivery-sequence/jurisdiction jurisdiction :delivery-sequence/next next-n}
                      {:delivery-record/seq (count (delivery-history s)) :delivery-record/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-commissions [s commissions]
    (when (seq commissions) (d/transact! conn (mapv commission->tx (vals commissions)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:commissions ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [commissions]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-commissions s commissions))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo commission set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
