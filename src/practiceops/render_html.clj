(ns practiceops.render-html
  "Build-time HTML renderer for docs/samples/operator-console.html.
  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300).
  Drives the REAL actor stack (practiceops.operation -> practiceops.governor
  -> practiceops.store). No invented numbers, no timestamps, byte-identical
  across reruns."
  (:require [clojure.string :as str]
            [practiceops.store :as store]
            [practiceops.operation :as op]
            [practiceops.phase :as phase]
            [practiceops.governor :as governor]
            [langgraph.graph :as g]))

(def ^:private operator {:actor-id "op-1" :actor-role :practice-principal :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn run-demo!
  "Drives the real OperationActor StateGraph through a scenario built
  directly from `practiceops.store/demo-data` and `practiceops.governor`'s
  actual rules (this repo's own `practiceops.sim` was run and checked
  against the real seed data / allowlist / violation names and found
  trustworthy -- this mirrors its scenario rather than reusing
  `sim/-main` directly, to keep this namespace's demo self-contained):

    1. `:commission/intake` commission-1 (JPN, clean, no professional
       seal required) -- `practiceops.phase`'s only phase-3 auto-
       eligible op -> auto-commits.
    2. `:jurisdiction/assess` commission-1 -- a write op enabled at
       phase 3 but not auto-eligible -> escalates (:phase-approval)
       -> human approval -> commit.
    3. `:design/verify` commission-1 -- `:actuation/verify-design` is
       a `governor/high-stakes` op, ALWAYS escalates even when clean
       -> human approval -> commit (drafts a real design-verification
       record via `practiceops.registry`).
    4. `:design/deliver` commission-1 -- `:actuation/deliver-
       certification` likewise always escalates -> human approval ->
       commit (drafts a real certification-delivery record).
    5. `:jurisdiction/assess` commission-2 with `:no-spec?` true --
       commission-2's own seeded jurisdiction is \"ATL\", which has NO
       entry in `practiceops.facts/catalog` -- HARD hold, rule
       `:no-spec-basis`.
    6. `:jurisdiction/assess` + `:design/verify` commission-3 (JPN, has
       spec-basis) -> commit, then `:design/deliver` commission-3 --
       commission-3 is seeded with `:claimed-fee 4000.0` but
       `:hours 30 :rate 120.0` (independent recompute 3600.0) -- HARD
       hold, rule `:fee-total-mismatch`.
    7. `:jurisdiction/assess` commission-4 -> commit, then
       `:design/verify` commission-4 -- commission-4 is seeded with
       `:within-code-scope? false` -- HARD hold, rule
       `:design-outside-scope`.
    8. `:jurisdiction/assess` + `:design/verify` commission-5 (JPN, has
       spec-basis) -> commit, then `:design/deliver` commission-5 --
       commission-5 is seeded with `:requires-professional-seal? true`
       and `:seal-authority-verified? false` -- HARD hold, rule
       `:professional-seal-invalid`.

  Returns the seeded `db` (a `practiceops.store/MemStore`) after the
  run, so `render` can read every value straight off it."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "t1" {:op :commission/intake :subject "commission-1"
                       :patch {:id "commission-1" :client "Kita Cooperative"}})

    (exec! actor "t2" {:op :jurisdiction/assess :subject "commission-1"})
    (approve! actor "t2")

    (exec! actor "t3" {:op :design/verify :subject "commission-1"})
    (approve! actor "t3")

    (exec! actor "t4" {:op :design/deliver :subject "commission-1"})
    (approve! actor "t4")

    (exec! actor "t5" {:op :jurisdiction/assess :subject "commission-2" :no-spec? true})

    (exec! actor "t6" {:op :jurisdiction/assess :subject "commission-3"})
    (approve! actor "t6")
    (exec! actor "t7" {:op :design/verify :subject "commission-3"})
    (approve! actor "t7")
    (exec! actor "t8" {:op :design/deliver :subject "commission-3"})

    (exec! actor "t9" {:op :jurisdiction/assess :subject "commission-4"})
    (approve! actor "t9")
    (exec! actor "t10" {:op :design/verify :subject "commission-4"})

    (exec! actor "t11" {:op :jurisdiction/assess :subject "commission-5"})
    (approve! actor "t11")
    (exec! actor "t12" {:op :design/verify :subject "commission-5"})
    (approve! actor "t12")
    (exec! actor "t13" {:op :design/deliver :subject "commission-5"})

    db))

;; ----------------------------- render helpers -----------------------------

(defn- esc
  "Minimal HTML-escape -- every rendered string passes through this."
  [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- last-fact-for
  "The most recent ledger fact for `subject-id`, off the real
  subject-key field this repo's `commit-fact`/`hold-fact` records use:
  `:subject` (see `practiceops.operation/commit-fact` and
  `practiceops.governor/hold-fact`)."
  [ledger subject-id]
  (last (filter #(= subject-id (:subject %)) ledger)))

(defn- status-cell
  "[css-class label] for the last known ledger fact of a subject --
  the same cond pattern used fleet-wide."
  [fact]
  (cond
    (nil? fact)                                 ["muted" "in progress"]
    (= :committed (:t fact))                    ["ok" "committed"]
    (= :approval-granted (:t fact))              ["ok" "approval-granted"]
    (= :governor-hold (:t fact))                 ["err" (str "governor-hold: " (str/join "," (map name (:basis fact))))]
    (= :approval-rejected (:t fact))             ["err" "approval-rejected"]
    (= :approval-requested (:t fact))            ["warn" "approval-requested"]
    :else                                        ["muted" "in progress"]))

(defn- commissions-table [db]
  (let [commissions (store/all-commissions db)]
    (str
     "<table>\n<thead><tr>\n"
     "<th>id</th><th>client</th><th>project type</th><th>jurisdiction</th>\n"
     "<th>within code/scope?</th><th>seal required?</th><th>seal verified?</th>"
     "<th>verified?</th><th>delivered?</th><th>status</th>\n"
     "</tr></thead>\n<tbody>\n"
     (str/join
      "\n"
      (for [c commissions
            :let [ledger (store/ledger db)
                  fact (last-fact-for ledger (:id c))
                  [cls label] (status-cell fact)]]
        (str "<tr>"
             "<td><code>" (esc (:id c)) "</code></td>"
             "<td>" (esc (:client c)) "</td>"
             "<td><code>" (esc (:project-type c)) "</code></td>"
             "<td>" (esc (:jurisdiction c)) "</td>"
             "<td>" (if (:within-code-scope? c) "yes" "<span class=\"critical\">no</span>") "</td>"
             "<td>" (if (:requires-professional-seal? c) "yes" "no") "</td>"
             "<td>" (if (:seal-authority-verified? c) "yes"
                       (if (:requires-professional-seal? c) "<span class=\"warn\">no</span>" "n/a")) "</td>"
             "<td>" (if (:verified? c) "yes" "no") "</td>"
             "<td>" (if (:delivered? c) "yes" "no") "</td>"
             "<td class=\"" cls "\">" (esc label) "</td>"
             "</tr>")))
     "\n</tbody></table>")))

(defn- committed-records-table [db]
  (let [verifications (store/verification-history db)
        deliveries (store/delivery-history db)]
    (str
     "<table>\n<thead><tr>\n"
     "<th>record_id</th><th>kind</th><th>commission_id</th><th>jurisdiction</th>\n"
     "</tr></thead>\n<tbody>\n"
     (str/join
      "\n"
      (for [r (concat verifications deliveries)]
        (str "<tr>"
             "<td><code>" (esc (get r "record_id")) "</code></td>"
             "<td>" (esc (get r "kind")) "</td>"
             "<td><code>" (esc (get r "commission_id")) "</code></td>"
             "<td>" (esc (get r "jurisdiction")) "</td>"
             "</tr>")))
     "\n</tbody></table>")))

(defn- action-gate-table
  "Static op-contract description, sourced from the real
  `practiceops.phase/phases` (phase 3, this actor's `default-phase`)
  and `practiceops.governor/high-stakes` -- not invented, just
  rendered."
  []
  (let [ph (get phase/phases phase/default-phase)]
    (str
     "<table>\n<thead><tr>\n"
     "<th>op</th><th>phase-" phase/default-phase " write allowed?</th><th>auto-eligible?</th><th>always escalates (high-stakes)?</th>\n"
     "</tr></thead>\n<tbody>\n"
     (str/join
      "\n"
      (for [op (sort phase/write-ops)]
        (str "<tr>"
             "<td><code>" (esc op) "</code></td>"
             "<td>" (if (contains? (:writes ph) op) "yes" "<span class=\"warn\">no</span>") "</td>"
             "<td>" (if (contains? (:auto ph) op) "<span class=\"ok\">yes</span>" "no") "</td>"
             "<td>" (if (contains? governor/high-stakes op) "<span class=\"critical\">yes</span>" "no") "</td>"
             "</tr>")))
     "\n</tbody></table>")))

(defn- audit-ledger-table [db]
  (str
   "<table>\n<thead><tr>\n"
   "<th>t</th><th>op</th><th>subject</th><th>disposition</th><th>basis / rule</th>\n"
   "</tr></thead>\n<tbody>\n"
   (str/join
    "\n"
    (for [f (store/ledger db)]
      (str "<tr>"
           "<td>" (esc (:t f)) "</td>"
           "<td><code>" (esc (:op f)) "</code></td>"
           "<td><code>" (esc (:subject f)) "</code></td>"
           "<td class=\""
           (case (:disposition f) :commit "ok" :hold "err" "muted")
           "\">" (esc (:disposition f)) "</td>"
           "<td>" (if (seq (:basis f))
                    (str/join ", " (map (comp esc name) (:basis f)))
                    "&mdash;")
           "</td>"
           "</tr>")))
   "\n</tbody></table>"))

(def ^:private css
  "table { width: 100%; border-collapse: collapse; font-size: 14px; }
.ok { color: #137a3f; }
body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }
header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }
th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }
h2 { margin-top: 0; font-size: 15px; }
.warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }
main { max-width: 980px; margin: 24px auto; padding: 0 20px; }
header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }
.muted { color: #888; font-size: 13px; }
.critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }
.card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }
th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }
header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }
code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }")

(defn render [db]
  (str
   "<!doctype html>\n"
   "<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n"
   "<title>practiceops.render-html -- Design & Certification Governor operator console</title>\n"
   "<style>\n" css "\n</style>\n"
   "</head>\n<body>\n"
   "<header class=\"bar\"><h1>Architectural &amp; Engineering Practice Governor -- Operator Console</h1>"
   "<span class=\"badge\">ISIC 7110 &middot; phase " phase/default-phase " (" (:label (get phase/phases phase/default-phase)) ")</span>"
   "</header>\n"
   "<main>\n"
   "<div class=\"card\">\n<h2>Commissions</h2>\n" (commissions-table db) "\n</div>\n"
   "<div class=\"card\">\n<h2>Committed records (design-verification / certification-delivery drafts)</h2>\n" (committed-records-table db) "\n</div>\n"
   "<div class=\"card\">\n<h2>Action gate (practiceops.phase &middot; practiceops.governor/high-stakes)</h2>\n" (action-gate-table) "\n</div>\n"
   "<div class=\"card\">\n<h2>Audit ledger</h2>\n" (audit-ledger-table db) "\n</div>\n"
   "</main>\n"
   "</body></html>\n"))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out)))
