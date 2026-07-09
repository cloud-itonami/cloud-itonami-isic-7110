(ns practiceops.registry
  "Pure-function design-verification + certification-delivery record
  construction -- an append-only architectural/engineering-practice
  book-of-record draft.

  Like every sibling actor's registry, there is no single international
  reference-number standard for a verification or delivery record --
  every practice/jurisdiction assigns its own reference format. This
  namespace does NOT invent one; it builds a jurisdiction-scoped
  sequence number and validates the record's required fields, the
  same honest, non-fabricating discipline `practiceops.facts` uses.

  `fee-total-matches-claim?` is an HONEST reapplication of the SAME
  ground-truth-recompute DISCIPLINE `hospitalityops.registry`'s own
  `folio-total-matches-claim?`, `agronomyops.registry`'s own `dose-
  matches-claim?`, `quarryops.registry`'s own `royalty-matches-claim?`
  and `retailops.registry`'s own `sale-total-matches-claim?` establish
  (verify a claimed monetary total against the entity's own recorded
  quantity x unit fields), reapplied to a commission's fee line rather
  than a folio, dose, royalty or retail-sale line -- not claimed as new
  code, though no literal code is shared (different domain).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real practice-management system. It builds the RECORD an
  operator would keep, not the act of verifying a design or delivering
  a certification itself (that is `practiceops.operation`'s `:design/
  verify`/`:design/deliver`, always human-gated -- see README
  `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the practice operator's act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn compute-fee-total
  "The ground-truth fee total for `commission`'s own `:hours` and
  `:rate` -- a single flat hours x rate calculation, not a full
  professional-fee engine with expenses/reimbursables."
  [{:keys [hours rate]}]
  (* (double hours) (double rate)))

(defn fee-total-matches-claim?
  "Does `commission`'s own `:claimed-fee` equal the independently
  recomputed `compute-fee-total`? A pure ground-truth check against
  the commission's own permanent fields -- see ns docstring for why
  this is an honest reapplication of the SAME discipline every sibling
  actor's own cost/total-matching check establishes, not a new
  concept."
  [{:keys [claimed-fee] :as commission}]
  (== (double claimed-fee) (compute-fee-total commission)))

(defn register-verification
  "Validate + construct the DESIGN-VERIFICATION registration DRAFT --
  the practice operator's own legal act of verifying a real design
  against code/scope. Pure function -- does not touch any real CAE/
  practice-management system; it builds the RECORD an operator would
  keep. `practiceops.governor` independently re-verifies the
  commission's own code-scope ground truth, and blocks a double-
  verification of the same record, before this is ever allowed to
  commit."
  [commission-id jurisdiction sequence]
  (when-not (and commission-id (not= commission-id ""))
    (throw (ex-info "verification: commission_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "verification: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "verification: sequence must be >= 0" {})))
  (let [verification-number (str (str/upper-case jurisdiction) "-VER-" (zero-pad sequence 6))
        record {"record_id" verification-number
                "kind" "verification-draft"
                "commission_id" commission-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "verification_number" verification-number
     "certificate" (unsigned-certificate "DesignVerification" verification-number verification-number)}))

(defn register-delivery
  "Validate + construct the CERTIFICATION-DELIVERY registration DRAFT
  -- the practice operator's own legal act of delivering a real
  stamped/certified deliverable to the client or public authority.
  Pure function -- does not touch any real practice-management
  system; it builds the RECORD an operator would keep.
  `practiceops.governor` independently re-verifies the commission's
  own fee/seal ground truth, and blocks a double-delivery of the same
  record, before this is ever allowed to commit."
  [commission-id jurisdiction sequence]
  (when-not (and commission-id (not= commission-id ""))
    (throw (ex-info "delivery: commission_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "delivery: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "delivery: sequence must be >= 0" {})))
  (let [delivery-number (str (str/upper-case jurisdiction) "-DLV-" (zero-pad sequence 6))
        record {"record_id" delivery-number
                "kind" "delivery-draft"
                "commission_id" commission-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "delivery_number" delivery-number
     "certificate" (unsigned-certificate "CertificationDelivery" delivery-number delivery-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
