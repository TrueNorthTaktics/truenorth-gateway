# Architecture

## Design goals

1. **The gateway is in the operator's pocket.** The engine runs inside an ATAK plugin on the handset, so a report can be built, translated and sent even when no server or headquarters network is reachable.
2. **Schema-driven.** Message types are data (JSON definitions). Forms, validation, every wire format and the XML schema are all derived from the same definition.
3. **Pluggable formats.** Each wire format is a `Codec`. A new standard, or an authoritative schema once it is available, is added by registering another codec. Existing code does not change.
4. **Link-aware.** Each radio bearer has a profile with its rate, fragment size, whether it is one-way, and its preferred format. Precedence controls how much forward error correction is added.
5. **Portable core.** The core is pure Java 11 with zero dependencies, and has no Android APIs in it. The same library can later run on a laptop, a vehicle computer or a headquarters server without being rewritten.

## Components

| Component | Package | Responsibility |
|---|---|---|
| Definitions | `model.DefinitionRegistry`, `model.DefinitionLoader` | Load message types from JSON; look up by id, binary code or XML name |
| Canonical message | `model.Message`, `model.FieldDef` | One validated, normalised in-memory form that every codec reads and writes |
| Codecs | `codec.*` | `cot`: Cursor-on-Target<br>`xml`: NIEM-style exchange plus a generated XSD<br>`slash`: character-oriented text<br>`bin`: bit-packed binary |
| Schema generation | `codec.XsdGenerator` | W3C XML Schema built from the definitions |
| Erasure coding | `fec.ReedSolomon`, `fec.Fragmenter` | Systematic RS over GF(2^8); the message can be rebuilt from any *k* of *n* fragments |
| Redundancy policy | `fec.RedundancyPolicy` | Parity ratio by precedence; this is the hook for ML-driven prioritisation |
| Link emulation | `link.LinkProfile`, `link.LossModel` | Representative bearers; random and Gilbert-Elliott burst loss |
| Facade | `Gateway` | `encode`, `decode`, `transcode` and `prepare(message, link)` |

## Data flow

```
operator input ─► Message ─► Codec(link.preferredFormat) ─► Fragmenter + RS parity ─► radio
radio ─► Reassembler (any k of n) ─► Codec.decode ─► Message ─► Codec(any other format) ─► C2 / allied system / map
```

## Hosting in ATAK (next stage)

The plugin wraps `core` unchanged and adds the following:

- **Forms generated from definitions.** Loading a new definition produces a new report form.
- **Voice and chat drafting.** Offline speech-to-text, in English and French, fills a draft that the operator confirms before it is sent.
- **Inbox and map.** Messages decoded from any format appear as CoT on the map.
- **Bearer selection and status.** The operator sees what was sent, over which link, and what has been acknowledged.

## Roadmap toward the remaining outcomes

| Outcome | Planned approach |
|---|---|
| Additional authoritative standards | A codec per standard (for example, NIEM 6 MilOps mappings, or MTF-XML when available), selected by configuration |
| Security labelling | STANAG 4774/4778-style confidentiality metadata on every message |
| Simulation and wargaming | Exercise feeds through CoT/UDP and a file import/export interface; a simulation bridge adapter |
| Service monitoring | OpenTelemetry metrics and events, plus an incident catalogue for IT service management integration |
| Decision support | A fused "evacuation options" view built from status messages received across domains |
| Burst-loss resilience | Interleaving across messages and adaptive parity driven by observed link quality |
