# cloud-itonami-7110

Open Business Blueprint for **ISIC Rev.5 7110**: architectural and engineering practice — design, simulation, verification and certification.

This repository designs a forkable OSS business for community architectural and engineering practice:
run by a qualified operator so a community keeps its own operating records
instead of renting a closed SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a field and drawing robot performs site survey, drafting and verification tasks under an actor that proposes
actions and an independent **Practice Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
handling stamped/certified drawings and public-works deliverables) require human sign-off.

## Core Contract

```text
intake + identity + cae records
        |
        v
Advisor -> Practice Governor -> proceed, hold, or human approval
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `7110`). Required capabilities:

- `:robotics`
- `:cae`
- `:identity`
- `:forms`
- `:dmn`
- `:bpmn`
- `:audit-ledger`

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
