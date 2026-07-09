(ns practiceops.practiceopsllm
  "PracticeOps-LLM client -- the *contained intelligence node* for the
  community-architectural/engineering-practice actor.

  It normalizes commission intake, drafts a per-jurisdiction building/
  engineering-code and professional-licensure evidence checklist,
  drafts the design-verification action, and drafts the certification-
  delivery action. CRITICAL: it is a smart-but-untrusted advisor. It
  returns a *proposal* (with a rationale + the fields it cited), never
  a committed record or a real verification/delivery. Every output is
  censored downstream by `practiceops.governor` before anything
  touches the SSoT, and `:design/verify`/`:design/deliver` proposals
  NEVER auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/verify-design | :actuation/deliver-certification | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [practiceops.facts :as facts]
            [practiceops.registry :as registry]
            [practiceops.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the client, hours/rate or jurisdiction. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "受託記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :commission/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-jurisdiction
  "Per-jurisdiction building/engineering-code and professional-
  licensure evidence checklist draft. `:no-spec?` injects the failure
  mode we must defend against: proposing a checklist for a
  jurisdiction with NO official spec-basis in `practiceops.facts` --
  the Design Governor must reject this (never invent a jurisdiction's
  requirements)."
  [db {:keys [subject no-spec?]}]
  (let [c (store/commission db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction c))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "practiceops.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :assessment/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-verification
  "Draft the actual DESIGN-VERIFICATION action -- verifying a real
  design against code/scope. ALWAYS `:stake :actuation/verify-design`
  -- this is a REAL-WORLD act (an engineer/architect signs off on
  analysis correctness), never a draft the actor may auto-run. See
  README `Actuation`: no phase ever adds this op to a phase's `:auto`
  set (`practiceops.phase`); the governor also always escalates on
  `:actuation/verify-design`. Two independent layers agree,
  deliberately."
  [db {:keys [subject]}]
  (let [c (store/commission db subject)]
    {:summary    (str subject " 向け設計検証提案"
                      (when c (str " (client=" (:client c) ")")))
     :rationale  (if c
                   (str "within-code-scope?=" (:within-code-scope? c)
                        " jurisdiction=" (:jurisdiction c))
                   "commissionが見つかりません")
     :cites      (if c [subject] [])
     :effect     :commission/mark-verified
     :value      {:commission-id subject}
     :stake      :actuation/verify-design
     :confidence (if (and c (:within-code-scope? c)) 0.9 0.3)}))

(defn- propose-delivery
  "Draft the actual CERTIFICATION-DELIVERY action -- delivering a real
  stamped/certified deliverable. ALWAYS `:stake :actuation/deliver-
  certification` -- this is a REAL-WORLD act (a legal instrument is
  handed to the client/public authority, fees settle), never a draft
  the actor may auto-run. See README `Actuation`: no phase ever adds
  this op to a phase's `:auto` set (`practiceops.phase`); the governor
  also always escalates on `:actuation/deliver-certification`. Two
  independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [c (store/commission db subject)
        fee-ok? (and c (registry/fee-total-matches-claim? c))
        seal-ok? (and c (or (not (:requires-professional-seal? c)) (:seal-authority-verified? c)))]
    {:summary    (str subject " 向け納品提案"
                      (when c (str " (client=" (:client c) ")")))
     :rationale  (if c
                   (str "claimed-fee=" (:claimed-fee c)
                        " independent-recompute=" (registry/compute-fee-total c)
                        " seal-ok?=" seal-ok?)
                   "commissionが見つかりません")
     :cites      (if c [subject] [])
     :effect     :commission/mark-delivered
     :value      {:commission-id subject}
     :stake      :actuation/deliver-certification
     :confidence (if (and fee-ok? seal-ok?) 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :commission/intake           (normalize-intake db request)
    :jurisdiction/assess              (assess-jurisdiction db request)
    :design/verify                        (propose-verification db request)
    :design/deliver                           (propose-delivery db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは地域建築設計・エンジニアリング事務所の検証・納品エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。"
       "説明や前置きは一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:commission/upsert|:assessment/set|:commission/mark-verified|"
       ":commission/mark-delivered) "
       ":stake(:actuation/verify-design か :actuation/deliver-certification か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"
       "設計の範囲適合性や専門技術者の押印権限を偽って報告してはいけません。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :jurisdiction/assess    {:commission (store/commission st subject)}
    :design/verify          {:commission (store/commission st subject)}
    :design/deliver         {:commission (store/commission st subject)}
    {:commission (store/commission st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Design Governor escalates/
  holds -- an LLM hiccup can never auto-verify a design or auto-
  deliver a certification."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :practiceopsllm-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
