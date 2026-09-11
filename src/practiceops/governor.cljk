(ns practiceops.governor
  "Design Governor -- the independent compliance layer that earns the
  PracticeOps-LLM the right to commit. The LLM has no notion of
  jurisdictional building/engineering-code or professional-licensure
  law, whether a commission's own claimed fee actually equals hours
  times rate, whether a proposed design actually falls within the
  applicable code/scope, whether a professional's seal authority has
  actually been verified for a deliverable that legally requires one,
  or when an act stops being a draft and becomes a real-world design
  verification or certification delivery, so this MUST be a separate
  system able to *reject* a proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is `:design-governor` -- NOT
  `:practice-governor`, because `:practice-governor` collides with
  `cloud-itonami-isic-8620`'s own (Clinical Practice Governor,
  medical/dental practice). Grep-verified BEFORE finalizing: this is
  the FIRST governor-name collision encountered across every build in
  this window's sequence (9511/4711/4920/0810/0162/5510 were all
  grep-verified unique). Rather than force a reuse-precedent
  discussion for an unrelated domain (clinical practice has nothing to
  do with architectural/engineering practice beyond sharing the word
  'practice'), this build picks a FRESH, distinct keyword grounded in
  this blueprint's own operating-states text ('intake : scope : design
  : verify : deliver : audit') -- `:design-governor`, grep-verified
  UNIQUE fleet-wide once chosen. This follows the SAME governed-actor
  architecture (langgraph StateGraph + independent Governor + Phase
  0->3 rollout) established by `cloud-itonami-isic-6511`.

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'design outside code/scope is blocked; verification
  evidence is required; certification is auditable') and its own
  docs/operator-guide.md ('handling stamped/certified drawings and
  public-works deliverables' requiring human sign-off) name exactly
  the checks below.

  Seven checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them. The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `practiceops.phase`: for `:stake
  :actuation/verify-design`/`:actuation/deliver-certification` (a real
  verification or delivery) NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`practiceops.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:design/verify`/`:design/
                                       deliver`, has the jurisdiction
                                       actually been assessed with a
                                       full evidence checklist on
                                       file?
    3. Design outside code/scope   -- for `:design/verify`,
                                       INDEPENDENTLY verify the
                                       commission's own `:within-code-
                                       scope?` is true -- the FLAGSHIP
                                       genuinely new check this
                                       vertical adds (grep-verified
                                       absent fleet-wide -- zero hits
                                       for 'design-outside-scope'/
                                       'code-compliance' as a governor
                                       check function name), the 82nd
                                       distinct application of the
                                       unconditional-evaluation
                                       discipline overall (most
                                       recently `hospitalityops.
                                       governor/guest-disclosure-
                                       authorization-unconfirmed-
                                       violations` at 81st; a
                                       CONCURRENT session also
                                       independently landed
                                       `construction.governor/permit-
                                       and-inspection-required` on
                                       `cloud-itonami-isic-4211` in
                                       this window, under its own
                                       separate numbering scheme --
                                       grep-verified no name collision
                                       with either). Grounded in real
                                       building/engineering-code law:
                                       Japan's own 建築基準法 (Building
                                       Standards Act, enforced by MLIT/
                                       prefectural governments), the
                                       US's International Building
                                       Code (IBC, as locally adopted --
                                       honestly labeled an ICC MODEL
                                       code, not federal statute), the
                                       UK's Building Regulations 2010
                                       (as amended by the Building
                                       Safety Act 2022), and Germany's
                                       Musterbauordnung (MBO, model
                                       building code, as implemented
                                       per Land) -- directly grounded
                                       in this blueprint's own text
                                       ('design outside code/scope is
                                       blocked'). Evaluated
                                       UNCONDITIONALLY (every
                                       verification needs a design
                                       within code/scope).
    4. Fee total mismatch          -- for `:design/deliver`,
                                       INDEPENDENTLY recompute whether
                                       the commission's own `:claimed-
                                       fee` equals `hours x rate`
                                       (`practiceops.registry/fee-
                                       total-matches-claim?`) -- an
                                       HONEST reapplication of the
                                       SAME ground-truth-recompute
                                       DISCIPLINE `hospitalityops.
                                       registry`'s/`agronomyops.
                                       registry`'s/`quarryops.
                                       registry`'s own checks
                                       establish, reapplied to a
                                       commission's fee line -- not
                                       claimed as new.
    5. Professional seal invalid   -- for `:design/deliver`, for a
                                       commission whose own record
                                       declares `:requires-
                                       professional-seal? true` (i.e.
                                       this deliverable is actually a
                                       public-works/regulatory
                                       instrument that legally requires
                                       a licensed professional's seal
                                       -- not every deliverable does,
                                       e.g. some preliminary/internal
                                       design memos do not),
                                       INDEPENDENTLY check whether
                                       `:seal-authority-verified?` is
                                       true. A GENUINELY NEW concept
                                       (grep-verified absent fleet-
                                       wide -- zero hits for
                                       'professional-seal'/'seal-
                                       authority' as a governor check
                                       function name; DISTINCT from
                                       `clinic.governor`'s own
                                       `credential-not-current-
                                       violations`, which checks a
                                       CLINICIAN's treatment-time
                                       license currency, not a sealed-
                                       deliverable's authority to be
                                       legally issued), the 83rd
                                       distinct application overall,
                                       the ELEVENTH conditional variant
                                       (after `socialresearch`/7220's,
                                       `bizassoc`/9411's, `training`/
                                       8549's, `furniture`/9524's,
                                       `specialtyrepair`/9529's,
                                       `leathergoods`/9523's,
                                       `ictrepair`/9511's, `quarryops`/
                                       0810's, `agronomyops`/0162's and
                                       `hospitalityops`/5510's own, at
                                       63rd, 64th, 66th, 67th, 68th,
                                       69th, 71st, 77th, 79th and
                                       81st). CONDITIONAL on the
                                       commission's own `:requires-
                                       professional-seal?` ground
                                       truth. Grounded in real
                                       professional-licensure/seal-
                                       authority law: Japan's own
                                       建築士法 (Architects and Building
                                       Engineers Act) Article 20, the
                                       US's state Professional
                                       Engineering Practice Acts
                                       (NCEES Model Law/Rules), the
                                       UK's Architects Act 1997
                                       (protected title 'architect',
                                       enforced by the Architects
                                       Registration Board), and
                                       Germany's state Architekten-/
                                       Ingenieurgesetze (enforced by
                                       Länder chambers) -- ALL FOUR
                                       seeded jurisdictions actually
                                       have a real regime here,
                                       reported honestly (a full-
                                       coverage sub-citation, matching
                                       `quarryops`/0810's own blast-
                                       safety and `agronomyops`/0162's
                                       own water-buffer full coverage
                                       rather than `hospitalityops`/
                                       5510's own honest single-
                                       jurisdiction gap).
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:design/verify`/
                                       `:design/deliver` (REAL acts)
                                       -> escalate.

  Two more guards, double-verification/double-delivery prevention, are
  enforced but NOT listed as numbered HARD checks above because they
  need no upstream comparison at all -- `already-verified-violations`/
  `already-delivered-violations` refuse to verify/deliver the SAME
  commission twice, off dedicated `:verified?`/`:delivered?` facts
  (never a `:status` value) -- the SAME 'check a dedicated boolean,
  not status' discipline every prior governor's guards establish,
  informed by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320)."
  (:require [practiceops.facts :as facts]
            [practiceops.registry :as registry]
            [practiceops.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Verifying a real design and delivering a real certification are the
  two real-world actuation events this actor performs -- a two-member
  set, matching every sibling's own dual-actuation shape."
  #{:actuation/verify-design :actuation/deliver-certification})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:design/verify`/`:design/deliver`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's building/engineering-code/professional-
  licensure requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :design/verify :design/deliver} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:design/verify`/`:design/deliver`, the jurisdiction's required
  intake/scope/design/verification evidence must actually be
  satisfied -- do not trust the advisor's self-reported confidence
  alone."
  [{:keys [op subject]} st]
  (when (contains? #{:design/verify :design/deliver} op)
    (let [c (store/commission st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction c) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(受託記録/範囲確定記録/設計記録/検証記録等)が充足していない状態での提案"}]))))

(defn- design-outside-scope-violations
  "For `:design/verify`, INDEPENDENTLY verify the commission's own
  `:within-code-scope?` is true -- the flagship genuinely new check
  this vertical adds. Evaluated UNCONDITIONALLY (every verification
  needs a design within code/scope)."
  [{:keys [op subject]} st]
  (when (= op :design/verify)
    (let [c (store/commission st subject)]
      (when-not (true? (:within-code-scope? c))
        [{:rule :design-outside-scope
          :detail (str subject " の設計が建築基準/範囲外")}]))))

(defn- fee-total-mismatch-violations
  "For `:design/deliver`, INDEPENDENTLY recompute whether the
  commission's own claimed fee equals hours x rate via
  `practiceops.registry/fee-total-matches-claim?` -- needs no proposal
  inspection or stored-verdict lookup at all, an honest reapplication
  of the same discipline every sibling actor's own cost/total-matching
  check establishes."
  [{:keys [op subject]} st]
  (when (= op :design/deliver)
    (let [c (store/commission st subject)]
      (when-not (registry/fee-total-matches-claim? c)
        [{:rule :fee-total-mismatch
          :detail (str subject " の申告報酬合計(" (:claimed-fee c)
                      ")が独立再計算値(" (registry/compute-fee-total c) ")と一致しない")}]))))

(defn- professional-seal-invalid-violations
  "For `:design/deliver`, for a commission whose own record declares
  `:requires-professional-seal? true`, INDEPENDENTLY check whether
  `:seal-authority-verified?` is true -- a genuinely new concept,
  CONDITIONAL on the commission's own `:requires-professional-seal?`
  ground truth (not every deliverable legally requires a professional
  seal)."
  [{:keys [op subject]} st]
  (when (= op :design/deliver)
    (let [c (store/commission st subject)]
      (when (and (true? (:requires-professional-seal? c))
                 (not (true? (:seal-authority-verified? c))))
        [{:rule :professional-seal-invalid
          :detail (str subject " は専門技術者の押印/証明を要するが権限確認が未完了 -- 納品提案は進められない")}]))))

(defn- already-verified-violations
  "For `:design/verify`, refuses to verify the SAME commission record
  twice, off a dedicated `:verified?` fact (never a `:status`
  value)."
  [{:keys [op subject]} st]
  (when (= op :design/verify)
    (when (store/commission-already-verified? st subject)
      [{:rule :already-verified
        :detail (str subject " は既に検証済み")}])))

(defn- already-delivered-violations
  "For `:design/deliver`, refuses to deliver the SAME commission's
  certification twice, off a dedicated `:delivered?` fact (never a
  `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :design/deliver)
    (when (store/commission-already-delivered? st subject)
      [{:rule :already-delivered
        :detail (str subject " は既に納品済み")}])))

(defn check
  "Censors a PracticeOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (design-outside-scope-violations request st)
                           (fee-total-mismatch-violations request st)
                           (professional-seal-invalid-violations request st)
                           (already-verified-violations request st)
                           (already-delivered-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
