(ns practiceops.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean commission
  through intake -> jurisdiction assessment -> design verification
  (escalate/approve/commit) -> certification delivery (escalate/
  approve/commit), then a SEPARATE clean seal-required commission
  through the same lifecycle (demonstrating the conditional
  professional-seal check passing cleanly), then shows HARD-hold
  scenarios: a jurisdiction with no spec-basis, a fee-total mismatch
  (verified first), a design outside code/scope, and an unverified
  professional seal on a seal-required commission, a double
  verification, and a double delivery.

  Like `retailops`/4711's, `freightops`/4920's, `quarryops`/0810's,
  `agronomyops`/0162's and `hospitalityops`/5510's own new checks,
  this actor's new checks (`design-outside-scope?`, `professional-
  seal-invalid?`) are evaluated directly at `:design/verify`/`:design/
  deliver` time rather than via a separate screening op -- a real
  verification/delivery decision validates code/scope compliance and
  seal authority at the point of the act itself. Each check is still
  exercised directly and independently below, one commission per
  HARD-hold scenario, following the SAME 'exercise the failure mode
  directly, never only via a happy-path actuation' discipline
  `parksafety`'s ADR-2607071922 Decision 5 and every sibling since
  establish."
  (:require [langgraph.graph :as g]
            [practiceops.store :as store]
            [practiceops.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :practice-principal :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== commission/intake commission-1 (JPN, clean, no seal required) ==")
    (println (exec-op actor "t1" {:op :commission/intake :subject "commission-1"
                                  :patch {:id "commission-1" :client "Kita Cooperative"}} operator))

    (println "== jurisdiction/assess commission-1 (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :jurisdiction/assess :subject "commission-1"} operator))
    (println (approve! actor "t2"))

    (println "== design/verify commission-1 (always escalates -- actuation/verify-design) ==")
    (let [r (exec-op actor "t3" {:op :design/verify :subject "commission-1"} operator)]
      (println r)
      (println "-- human practice principal approves --")
      (println (approve! actor "t3")))

    (println "== design/deliver commission-1 (always escalates -- actuation/deliver-certification) ==")
    (let [r (exec-op actor "t4" {:op :design/deliver :subject "commission-1"} operator)]
      (println r)
      (println "-- human practice principal approves --")
      (println (approve! actor "t4")))

    (println "== commission/intake commission-6 (JPN, clean, seal required and verified) ==")
    (println (exec-op actor "t5" {:op :commission/intake :subject "commission-6"
                                  :patch {:id "commission-6" :client "Chuo Cooperative"}} operator))

    (println "== jurisdiction/assess commission-6 (escalates -- human approves) ==")
    (println (exec-op actor "t6" {:op :jurisdiction/assess :subject "commission-6"} operator))
    (println (approve! actor "t6"))

    (println "== design/verify commission-6 (always escalates) ==")
    (println (exec-op actor "t6b" {:op :design/verify :subject "commission-6"} operator))
    (println (approve! actor "t6b"))

    (println "== design/deliver commission-6 (seal required, verified -- escalates -- human approves) ==")
    (println (exec-op actor "t7" {:op :design/deliver :subject "commission-6"} operator))
    (println (approve! actor "t7"))

    (println "== jurisdiction/assess commission-2 (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t8" {:op :jurisdiction/assess :subject "commission-2" :no-spec? true} operator))

    (println "== jurisdiction/assess commission-3 (escalates -- human approves; sets up the fee-mismatch test) ==")
    (println (exec-op actor "t9" {:op :jurisdiction/assess :subject "commission-3"} operator))
    (println (approve! actor "t9"))

    (println "== design/verify commission-3 (always escalates) ==")
    (println (exec-op actor "t9b" {:op :design/verify :subject "commission-3"} operator))
    (println (approve! actor "t9b"))

    (println "== design/deliver commission-3 (claimed 4000.0 vs recompute 3600.0 -> HARD hold) ==")
    (println (exec-op actor "t10" {:op :design/deliver :subject "commission-3"} operator))

    (println "== jurisdiction/assess commission-4 (escalates -- human approves; sets up the outside-scope test) ==")
    (println (exec-op actor "t11" {:op :jurisdiction/assess :subject "commission-4"} operator))
    (println (approve! actor "t11"))

    (println "== design/verify commission-4 (design outside code/scope -> HARD hold) ==")
    (println (exec-op actor "t12" {:op :design/verify :subject "commission-4"} operator))

    (println "== jurisdiction/assess commission-5 (escalates -- human approves; sets up the professional-seal test) ==")
    (println (exec-op actor "t13" {:op :jurisdiction/assess :subject "commission-5"} operator))
    (println (approve! actor "t13"))

    (println "== design/verify commission-5 (always escalates) ==")
    (println (exec-op actor "t13b" {:op :design/verify :subject "commission-5"} operator))
    (println (approve! actor "t13b"))

    (println "== design/deliver commission-5 (seal required, unverified -> HARD hold) ==")
    (println (exec-op actor "t14" {:op :design/deliver :subject "commission-5"} operator))

    (println "== design/verify commission-1 AGAIN (double-verification -> HARD hold) ==")
    (println (exec-op actor "t15" {:op :design/verify :subject "commission-1"} operator))

    (println "== design/deliver commission-1 AGAIN (double-delivery -> HARD hold) ==")
    (println (exec-op actor "t16" {:op :design/deliver :subject "commission-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft verification records ==")
    (doseq [r (store/verification-history db)] (println r))

    (println "== draft delivery records ==")
    (doseq [r (store/delivery-history db)] (println r))))
