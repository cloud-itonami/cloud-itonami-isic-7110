# cloud-itonami-isic-7110

Open Business Blueprint for **ISIC Rev.5 7110**: architectural and
engineering practice -- design, simulation, verification and
certification.

This repository publishes a community-architectural/engineering-
practice actor -- commission intake, per-jurisdiction building/
engineering-code and professional-licensure regulatory assessment,
design verification and certification delivery -- as an OSS business
that any qualified operator can fork, deploy, run, improve and sell,
so a small practice or public-works program never surrenders design
and certification records to a closed SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet (92 prior actors) -- here it is
**PracticeOps-LLM ⊣ Design Governor**. This blueprint's own
`:itonami.blueprint/governor` keyword is `:design-governor` --
originally `:practice-governor`, renamed after discovering it collided
with `cloud-itonami-isic-8620`'s own Clinical Practice Governor (the
FIRST governor-name collision in this fleet's build sequence).
`:design-governor` is grep-verified UNIQUE fleet-wide.

> **Why an actor layer at all?** An LLM is great at drafting a
> commission summary, normalizing records, and checking whether a
> claimed fee actually equals a commission's own recorded hours times
> rate -- but it has **no notion of which jurisdiction's building/
> engineering-code or professional-licensure law is official, no
> license to verify a real design or deliver a real certification,
> and no way to know on its own whether a proposed design actually
> falls within the applicable code/scope or whether a professional's
> seal authority has actually been verified for a deliverable that
> legally requires one**. Letting it verify a design or deliver a
> certification directly invites fabricated regulatory citations, a
> fee mismatch being charged to a client, a design outside code/scope
> being verified as compliant, and a public-works deliverable being
> issued without a valid professional seal -- exposing the practice to
> real regulatory and professional-liability exposure. This project
> seals the PracticeOps-LLM into a single node and wraps it with an
> independent **Design Governor**, a human **approval workflow**, and
> an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers commission intake through building/engineering-code
and professional-licensure regulatory assessment, design verification
and certification delivery. It does **not**, by itself, hold any
professional license required to practice architecture/engineering in
a given jurisdiction, and it does not claim to. It also does not
perform the actual CAE/CFD analysis or drafting work itself, or judge
design quality -- `practiceops.registry/fee-total-matches-claim?` is a
pure ground-truth recompute against the commission's own recorded
fields, not a design-quality judgment. Whoever deploys and operates a
live instance (a qualified practice principal/licensed professional)
supplies any jurisdiction-specific license, the real CAE/CFD analysis
tooling and the real practice-management-system integrations, and
bears that jurisdiction's liability -- the software supplies the
governed, spec-cited, audited execution scaffold so that operator does
not have to build the compliance layer from scratch.

### Actuation

**Verifying a real design and delivering a real certification are
never autonomous, at any phase, by construction.** Two independent
layers enforce this (`practiceops.governor`'s `:actuation/verify-
design`/`:actuation/deliver-certification` high-stakes gate and
`practiceops.phase`'s phase table, which never puts either op in any
phase's `:auto` set) -- see `practiceops.phase`'s docstring and
`test/practiceops/phase_test.clj`'s `design-verify-never-auto-at-any-
phase`/`design-deliver-never-auto-at-any-phase`. The actor may draft,
check and recommend; a human practice principal/licensed professional
is always the one who actually verifies a design or delivers a
certification. Grounded directly in this blueprint's own `docs/
business-model.md` Trust Controls text ("design outside code/scope is
blocked; verification evidence is required; certification is
auditable") -- a genuine DUAL-actuation shape, applied SEQUENTIALLY to
the SAME commission record (verify first, deliver later), matching
`freightops`/4920's, `quarryops`/0810's, `agronomyops`/0162's and
`hospitalityops`/5510's own sequential shape rather than `retailops`/
4711's own alternative-kind shape.

## The core contract

```
commission intake + jurisdiction facts (practiceops.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ PracticeOps-LLM       │ ─────────────▶ │ Design Governor               │  (independent system)
   │ (sealed)              │  + citations    │ spec-basis · evidence-       │
   └───────────────────────┘                 │ incomplete · design-          │
          │                 commit ◀┼ outside-scope (FLAGSHIP NEW) ·        │
          │                         │ fee-total-mismatch (ground-truth)     │
    record + ledger        escalate ┼ · professional-seal-invalid               │
          │              (ALWAYS for│ (conditional, NEW) · already-             │
          │       :actuation/verify-│ verified · already-delivered              │
          │       design/           │                                            │
          │       :actuation/deliver│                                            │
          │       -certification}    │                                            │
          ▼                          └───────────────────────┘
      human approval
```

**The PracticeOps-LLM never verifies a design or delivers a
certification the Design Governor would reject, and never does so
without a human sign-off.** Hard violations (fabricated regulatory
requirements; unsupported evidence; a design outside code/scope; a
fee-total mismatch; an unverified professional seal on a seal-
required commission; a double verification/delivery) force **hold**
and *cannot* be approved past; a clean verification/delivery proposal
still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk two clean verify+deliver lifecycles (no seal required, seal required-and-verified), plus four HARD-hold cases, through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a field and drawing robot
performs site survey, drafting and verification tasks, under the
actor, gated by the independent **Design Governor**. The governor
never dispatches hardware itself; `:high`/`:safety-critical` actions
(such as handling stamped/certified drawings and public-works
deliverables) require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Design Governor, verification/delivery draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`7110`). This vertical's commission/design records are practice-
specific rather than a shared cross-operator data contract, so
`practiceops.*` runs on the generic robotics/cae/identity/forms/dmn/
bpmn/audit-ledger stack only -- no bespoke domain capability lib to
reference at all (unlike `retailops`/4711's own `kotoba-lang/retail`
and `freightops`/4920's own `kotoba-lang/logistics` integrations;
`kotoba-lang/cad`, `kami-cad-import`, `kami-engine-cae-solver` and
`drawingml` are generic CAE/CAD technology substrate resolved via
`kotoba.technology`'s own `:cae` capability, not a bespoke domain
capability library, matching `quarryops`/0810's, `agronomyops`/0162's
and `hospitalityops`/5510's own investigated-and-ruled-out precedent).

## Layout

| File | Role |
|---|---|
| `src/practiceops/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + verification AND delivery history (dual history). The double-actuation guard checks dedicated `:verified?`/`:delivered?` booleans rather than a `:status` value |
| `src/practiceops/registry.cljc` | Verification/delivery draft records, plus `fee-total-matches-claim?` -- an honest reapplication of the SAME ground-truth-recompute discipline every sibling actor's own cost/total-matching check establishes |
| `src/practiceops/facts.cljc` | Per-jurisdiction building/engineering-code AND professional-licensure catalog with an official spec-basis citation per entry, honest coverage reporting -- ALL FOUR seeded jurisdictions have a professional-seal sub-citation here |
| `src/practiceops/practiceopsllm.cljc` | **PracticeOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/jurisdiction-assessment/verification/delivery proposals |
| `src/practiceops/governor.cljc` | **Design Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · design-outside-scope, FLAGSHIP NEW, the 82nd unconditional-evaluation-discipline grounding · fee-total-mismatch · professional-seal-invalid, CONDITIONAL, the 83rd grounding) + 2 double-actuation guards + 1 soft (confidence/actuation gate) |
| `src/practiceops/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted assess → supervised (verify/deliver always human; commission intake is the ONLY auto-eligible op, no direct professional-liability risk) |
| `src/practiceops/operation.cljc` | **OperationActor** -- langgraph StateGraph |
| `src/practiceops/sim.cljc` | demo driver |
| `test/practiceops/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers commission intake through building/engineering-code
and professional-licensure regulatory assessment, design verification
and certification delivery -- the core governed lifecycle this
blueprint's own `docs/business-model.md` names in its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Commission intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:commission/intake`/`:jurisdiction/assess`) | Real CAE/CFD analysis tooling, real design-quality judgment (see `practiceops.facts`'s docstring) |
| Design verification, HARD-gated on full evidence and code/scope compliance, plus a double-verification guard (`:actuation/verify-design`) | |
| Certification delivery, HARD-gated on full evidence, a matching fee claim and (when applicable) a verified professional seal, plus a double-delivery guard (`:actuation/deliver-certification`) | |
| Immutable audit ledger for every intake/assessment/verification/delivery decision | |

Extending coverage is additive: add the next gate (e.g. a
peer-review-completion-verification check) as its own governed op
with its own HARD checks and tests, following the SAME "an independent
governor re-verifies against the actor's own records before any
real-world act" pattern this repo's flagship ops already establish.

## Jurisdiction coverage (honest)

`practiceops.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `practiceops.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `practiceops.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger. Note that the professional-seal sub-
citation is FULL coverage rather than a gap: ALL FOUR seeded
jurisdictions (JPN, USA, GBR, DEU) actually have a real professional-
licensure/seal-authority regime, reported honestly.

## Maturity

`:implemented` -- `PracticeOps-LLM` + `Design Governor` run as real,
tested code (see `Run` above), promoted from the originally-published
`:blueprint`-tier scaffold, following the SAME governed-actor
architecture as the 92 other prior actors across this fleet, with its
own distinct, independently-named governor. See
`docs/adr/0001-architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
