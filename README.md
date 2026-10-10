# TrueNorth Gateway

**A schema-driven tactical messaging engine that runs inside an ATAK plugin.** It turns operator reports into structured, standards-based messages, translates them between formats, and gets them through degraded radio links using Reed-Solomon erasure coding.

Developed by True North Taktics Inc. (Ottawa, Canada).

> **Status: proof of concept of the critical functions.** Tested in the lab over emulated links. The ATAK plugin user interface is the next stage of work (see [`atak-plugin/`](atak-plugin/README.md)). All schemas, message structures and data in this repository are **representative and synthetic**. No official NATO, MTF-XML, USMTF or NIEM schema, and no controlled or government-furnished material, has been used.

---

## The problem

Reports from the field are often passed by voice or chat. Someone then retypes them into formal military products for headquarters, other services and allies. Radio links are slow, intermittent and sometimes one-way, and verbose XML does not fit through them.

## What this proof of concept demonstrates

| Critical function | Evidence in this repo |
|---|---|
| **Schema-driven messages:** a new report type is a definition file, not new code | [`defs/medevac9.json`](core/src/main/resources/ca/truenorthtak/gateway/defs/medevac9.json), [`defs/spotrep.json`](core/src/main/resources/ca/truenorthtak/gateway/defs/spotrep.json) |
| **Four interchangeable formats**, with lossless round trips between any of them | Cursor-on-Target, NIEM-style XML, character-oriented slash text, compact binary. See [`CodecRoundTripTest`](core/src/test/java/ca/truenorthtak/gateway/CodecRoundTripTest.java) |
| **Standards-based validation** | The XML schema is generated from the definitions, and every output validates against it ([`schema/`](schema/gateway-exchange-1.xsd)) |
| **Character-oriented processing**, both directions | ADatP-3-style slash text, parsed and generated ([`examples/medevac9.slash.txt`](examples/medevac9.slash.txt)) |
| **Low-bandwidth packing** | A 9-line MEDEVAC goes from 1,359 B in CoT to **74 B** in binary (−95%) |
| **Delivery without retransmission** over lossy one-way links | Reed-Solomon erasure coding with precedence-based redundancy |

## Key results

Full output: [`results/BENCHMARK.md`](results/BENCHMARK.md). It is reproducible with a fixed seed and 10,000 trials per scenario.

**Size:** 9-line MEDEVAC request

| Format | Bytes | HF @ 2400 bps | VLF @ 50 bps |
|---|---:|---:|---:|
| Cursor-on-Target XML | 1,359 | 4.5 s | 3.6 min |
| Character-oriented slash text | 214 | 0.7 s | 34 s |
| Compact binary | **74** | **0.25 s** | **12 s** |

**Delivery over a one-way VLF broadcast** (receivers cannot ask for a resend)

| Random fragment loss | Without FEC | With Reed-Solomon FEC |
|---|---:|---:|
| 10% | 38.8% | **100.0%** |
| 20% | 13.2% | **99.9%** |
| 30% | 3.7% | **98.1%** |
| 40% | 1.0% | **86.7%** |

**Processing cost:** encoding and decoding take under 0.25 ms per message on a desktop JVM, which is light enough for a handset.

## Architecture in one picture

```
 ATAK plugin (Android)                       this repository: core/ (pure Java, no dependencies)
 ┌────────────────────┐   Message   ┌──────────────────────────────────────────────────────────┐
 │ forms from defs    │ ──────────► │ DefinitionRegistry ─► Message (canonical, validated)      │
 │ voice/chat drafts  │             │        │                                                  │
 │ map + inbox        │ ◄────────── │ CodecRegistry: cot │ xml (+XSD) │ slash │ bin  ◄─ plug-in │
 └────────────────────┘             │        │                                                  │
                                    │ Fragmenter + Reed-Solomon ◄─ RedundancyPolicy(precedence) │
                                    │        │                                                  │
                                    │ LinkProfile: VLF │ HF │ SATCOM │ MANET (preferred format) │
                                    └──────────────────────────────────────────────────────────┘
```

More detail is in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). The formats and API are specified in [`docs/INTERFACES.md`](docs/INTERFACES.md), and the link assumptions are in [`docs/LINK_PROFILES.md`](docs/LINK_PROFILES.md).

## Using it

```java
Gateway gw = Gateway.withDefaults();

Message m = gw.decode(cotBytes, "cot");                    // from ATAK
byte[] slash = gw.encode(m, "slash");                      // for an HF teletype channel
byte[] xml   = gw.transcode(cotBytes, "cot", "xml");       // for a C2 system
List<byte[]> fragments = gw.prepare(m, LinkProfile.VLF_BROADCAST);  // packed + FEC

Fragmenter.Reassembler r = new Fragmenter.Reassembler();   // at the receiver
byte[] payload = null;
for (byte[] f : received) { byte[] p = r.accept(f); if (p != null) payload = p; }
```

## Build and run

Requires JDK 11 or newer.

```bash
./gradlew build          # compile + 14 unit tests
./gradlew :bench:run     # regenerate results/, examples/ and schema/
```

The `core` library targets Java 11 bytecode and has **no runtime dependencies**, so it can be embedded unchanged in an Android ATAK plugin. Its only dependency is JUnit, used for tests.

## Repository layout

```
core/       message engine: definitions, codecs, Reed-Solomon FEC, link profiles
bench/      reproducible benchmark that produces results/, examples/ and schema/
examples/   each sample message in every format (generated)
schema/     XML schema generated from the definitions
results/    benchmark output
docs/       architecture, interface specifications, link assumptions, provenance
atak-plugin/  planned Android plugin module
```

## Copyright

Copyright © 2026 True North Taktics Inc. All rights reserved. See [`NOTICE`](NOTICE).
