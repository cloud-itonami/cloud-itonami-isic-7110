# ADR-0001: PracticeOps-LLM ⊣ Design Governor architecture

## Status

Accepted. `cloud-itonami-isic-7110` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-7110` publishes an OSS business blueprint for
community architectural and engineering practice (design, simulation,
verification and certification). Like every prior actor in this
fleet, the blueprint alone is not an implementation: this ADR records
the governed-actor architecture that promotes it to real, tested code,
following the same langgraph StateGraph + independent Governor + Phase
0→3 rollout pattern established by `cloud-itonami-isic-6511` (life
insurance) and applied across 91 prior siblings, most recently
`cloud-itonami-isic-5510` (community accommodation operations).

A `kotoba-lang` org search for cae/engineering/architecture/cad/
drafting/drawingml-named repos surfaced `cad`, `kami-cad-import`,
`kami-engine-cae-solver` and `drawingml`. All four are verified to be
GENERIC CAE/CAD/geometry TECHNOLOGY SUBSTRATE (the same tier as
`:robotics` -- resolved via `kotoba.technology`'s own capability
stack, which this blueprint's own `:required-technologies` already
declares as `:cae`), not a bespoke domain capability library for
architectural/engineering PRACTICE business records (comparable to
`kotoba-lang/retail`'s `ean13-valid?` or `kotoba-lang/logistics`'s
`tracking-valid?`). `com-cadence-eda` is also a vendor-protocol
compat shim (matching `com-opera-pms`'s own pattern), not domain
logic. This build returns to self-contained domain logic, the same
pattern the majority of this fleet's actors use.

**Governor-name collision (first in this build sequence)**: this
blueprint's own `:itonami.blueprint/governor` keyword was originally
`:practice-governor`, which collides with `cloud-itonami-isic-8620`'s
own (Clinical Practice Governor, medical/dental practice) -- the FIRST
governor-name collision encountered across every build in this
window's sequence (9511/4711/4920/0810/0162/5510 were all
grep-verified unique on first attempt). Since clinical practice and
architectural/engineering practice share nothing beyond the word
"practice," this build picks a fresh, distinct keyword grounded in the
blueprint's own operating-states text ("intake : scope : design :
verify : deliver : audit") rather than forcing an unrelated
reuse-precedent discussion: `:design-governor`, grep-verified UNIQUE
fleet-wide once chosen.

## Decision

### Decision 1: fresh governor identity after a name collision

`:practice-governor` (the blueprint's original keyword) collides with
`cloud-itonami-isic-8620`'s own. Renamed to `:design-governor`,
grep-verified unique fleet-wide, grounded in this blueprint's own
"design" operating state.

### Decision 2: dual-actuation shape, SEQUENTIAL on the SAME `commission` entity

This blueprint's own operating states ("intake : scope : design :
verify : deliver : audit") name two real-world professional-liability
acts: verifying a design and delivering a certification. These apply
SEQUENTIALLY to the SAME `commission` entity -- verify first, deliver
later -- matching `freightops`/4920's, `quarryops`/0810's,
`agronomyops`/0162's and `hospitalityops`/5510's own sequential shape
rather than `retailops`/4711's own alternative-kind shape.
`high-stakes` is `#{:actuation/verify-design :actuation/
deliver-certification}`.

### Decision 3: `fee-total-matches-claim?` -- an honest reapplication of the ground-truth-recompute discipline

`practiceops.registry/fee-total-matches-claim?` (commission's own
claimed fee vs. hours x rate) applies the SAME discipline
`hospitalityops.registry`'s own `folio-total-matches-claim?`,
`agronomyops.registry`'s own `dose-matches-claim?`, `quarryops.
registry`'s own `royalty-matches-claim?` and `retailops.registry`'s
own `sale-total-matches-claim?` establish -- verify a claimed monetary
total against the entity's own recorded fields, independent of
proposal inspection. No literal code is shared (different domain),
but the discipline is the same, documented as such rather than
claimed as a novel invention.

### Decision 4: entity and op shape

The primary entity is a `commission`. Four ops: `:commission/intake`
(directory upsert, no professional-liability risk),
`:jurisdiction/assess` (per-jurisdiction building/engineering-code
and professional-licensure evidence checklist, never auto),
`:design/verify` (POSITIVE, high-stakes), and `:design/deliver`
(POSITIVE, high-stakes).

### Decision 5: `design-outside-scope?` -- the 82nd unconditional-evaluation grounding, the FLAGSHIP genuinely new check

Grep-verified absent fleet-wide (zero hits for `design-outside-scope`,
`code-compliance` as a governor check name). Grounded in real
building/engineering-code law: Japan's own 建築基準法 (Building
Standards Act, enforced by MLIT/prefectural governments), the US's
International Building Code (IBC, as locally adopted -- honestly
labeled an ICC MODEL code, not federal statute), the UK's Building
Regulations 2010 (as amended by the Building Safety Act 2022), and
Germany's Musterbauordnung (MBO, model building code, as implemented
per Land) -- directly grounded in this blueprint's own text ("design
outside code/scope is blocked"). Evaluated UNCONDITIONALLY on every
`:design/verify` (every verification needs a design within code/
scope). A CONCURRENT session independently landed `construction.
governor/permit-and-inspection-required` on `cloud-itonami-isic-4211`
in this same window, under its own separate numbering scheme --
grep-verified no name collision with either check.

### Decision 6: `professional-seal-invalid?` -- the 83rd unconditional-evaluation grounding, the ELEVENTH conditional variant

Before writing this check, every prior sibling's governor namespace
was grepped for any check function named `professional-seal` or
`seal-authority` -- zero hits, confirming this is a genuinely new
concept, DISTINCT from `clinic.governor`'s own
`credential-not-current-violations` (which checks a CLINICIAN's
treatment-time license currency, not a sealed-deliverable's legal
authority to be issued). This is the ELEVENTH conditional variant
(after `socialresearch`/7220's, `bizassoc`/9411's, `training`/8549's,
`furniture`/9524's, `specialtyrepair`/9529's, `leathergoods`/9523's,
`ictrepair`/9511's, `quarryops`/0810's, `agronomyops`/0162's and
`hospitalityops`/5510's own, at 63rd, 64th, 66th, 67th, 68th, 69th,
71st, 77th, 79th and 81st) -- CONDITIONAL on the commission's own
`:requires-professional-seal?` ground truth: not every deliverable
legally requires a professional seal (e.g. preliminary/internal design
memos do not). Grounded in real professional-licensure/seal-authority
law: Japan's own 建築士法 (Architects and Building Engineers Act)
Article 20, the US's state Professional Engineering Practice Acts
(NCEES Model Law/Rules), the UK's Architects Act 1997 (protected title
"architect," enforced by the Architects Registration Board), and
Germany's state Architekten-/Ingenieurgesetze (enforced by Länder
chambers). ALL FOUR seeded jurisdictions actually have a real regime
here, reported honestly -- a full-coverage sub-citation, matching
`quarryops`/0810's own blast-safety and `agronomyops`/0162's own
water-buffer full coverage rather than `hospitalityops`/5510's own
honest single-jurisdiction gap.

### Decision 7: dedicated double-actuation-guard booleans

`:verified?`/`:delivered?` are dedicated booleans on the `commission`
record, never a single `:status` value -- the same discipline every
prior governor's guards establish, informed by
`cloud-itonami-isic-6492`'s real status-lifecycle bug
(ADR-2607071320).

### Decision 8: Store protocol, MemStore + DatomicStore parity

`practiceops.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in
`test/practiceops/store_contract_test.clj`.

### Decision 9: no bespoke domain capability lib; a genuine `blueprint.edn` field-sync gap found and fixed

Verified explicitly this session: `cad`/`kami-cad-import`/`kami-
engine-cae-solver`/`drawingml` are generic CAE/CAD technology
substrate (the `:cae` capability this blueprint already declares as
required), not a bespoke domain capability library for architectural/
engineering practice records; `com-cadence-eda` is a vendor-protocol
compat shim, matching `com-opera-pms`'s own pattern. This repo's
`blueprint.edn` had the correct `:required-technologies` matching the
`kotoba-lang/industry` registry's own entry for `"7110"` exactly, but
was MISSING `:optional-technologies [:cfd :optimization]` entirely --
the same gap pattern `agronomyops`/0162's and `hospitalityops`/5510's
own builds found. Fixed cleanly in the same commit as the `:maturity`
flip and the `:governor` rename.

### Decision 10: mock + LLM advisor pair

`practiceops.practiceopsllm` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-verifying
a design or auto-delivering a certification).

## Alternatives considered

- **Keeping `:practice-governor` and documenting a reuse precedent
  with `clinic.governor`**: rejected -- clinical practice and
  architectural/engineering practice share nothing beyond the word
  "practice"; a reuse-precedent discussion would be misleading rather
  than honest. A fresh, distinct keyword is the correct choice.
- **An unconditional professional-seal check** (applying to every
  delivery regardless of whether the deliverable actually requires a
  seal). Rejected: some deliverables (preliminary/internal design
  memos) have no seal requirement at all -- forcing the check onto
  every delivery would fabricate a requirement.
- **Treating `cad`/`kami-cad-import`/`kami-engine-cae-solver`/
  `drawingml` as this vertical's capability library.** Considered and
  explicitly ruled out: they are generic CAE/CAD technology substrate,
  not domain-specific business logic for architectural/engineering
  practice.

## Consequences

- 93rd actor in this fleet (92 implemented before this build).
- Establishes two genuinely NEW unconditional-evaluation-discipline
  checks: `design-outside-scope?` (FLAGSHIP, 82nd distinct application
  overall) and `professional-seal-invalid?` (83rd distinct application
  overall, the ELEVENTH conditional variant).
- `MemStore` ‖ `DatomicStore` parity is proven by
  `test/practiceops/store_contract_test.clj`.
- 39 tests / 176 assertions pass; lint is clean; the demo
  (`clojure -M:dev:run`) walks two clean verify+deliver lifecycles
  (no seal required, seal required-and-verified), plus four HARD-hold
  scenarios, end-to-end.
- `blueprint.edn` needed both a genuine field-sync fix (a missing
  `:optional-technologies [:cfd :optimization]` key) and a governor-
  keyword rename (`:practice-governor` -> `:design-governor`) in
  addition to the `:maturity` flip.

## References

- `cloud-itonami-isic-6511/docs/adr/0001-architecture.md` (origin of
  the general governed-actor architecture pattern)
- `cloud-itonami-isic-5510/docs/adr/0001-architecture.md` (most recent
  prior sibling, template for this ADR's structure)
- `cloud-itonami-isic-8620/src/clinic/governor.cljc` (the
  `:practice-governor` collision this build resolved)
- 建築基準法 (Building Standards Act); 建築士法 (Architects and Building
  Engineers Act) Article 20 (Japan)
- International Building Code (IBC), as locally adopted; NCEES Model
  Law/Rules (US)
- Building Regulations 2010 / Building Safety Act 2022; Architects Act
  1997 (UK)
- Musterbauordnung (MBO); state Architekten-/Ingenieurgesetze (Germany)
