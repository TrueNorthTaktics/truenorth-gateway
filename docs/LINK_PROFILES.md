# Link profiles and test conditions

The profiles are **representative, unclassified planning assumptions** for emulation. They do not describe the measured performance of any specific radio, modem or network. Each value is easy to change in [`LinkProfile.java`](../core/src/main/java/ca/truenorthtak/gateway/link/LinkProfile.java).

| Profile | Rate used | Fragment size | One-way | Preferred format | Rationale |
|---|---:|---:|:---:|---|---|
| VLF/LF broadcast | 50 bps | 16 B | yes | `bin` | VLF/LF broadcasts carry tens of bits per second, and receivers (for example submarines) do not transmit |
| HF data link | 2,400 bps | 64 B | no | `bin` | 2,400 bps is a common HF data rate; HF modems range from roughly 75 to 9,600 bps depending on conditions |
| Narrowband SATCOM | 9,600 bps | 256 B | no | `slash` | Narrowband channels in the low-kbps range |
| UHF tactical MANET | 1 Mbps | 1,024 B | no | `cot` | A conservative figure for IP MANET radios at the tactical edge |

Airtime figures ignore modem preambles, framing and link-layer coding. Those add a roughly constant overhead per transmission, which makes small messages even more valuable.

## Loss models

- **Random:** each fragment is lost independently with probability *p* (10–40% tested).
- **Burst (Gilbert-Elliott):** a two-state channel. The good-to-bad probability per fragment is 0.10, the bad-to-good probability is 0.30, and 80% of fragments are lost while bad. The average loss is 20%. This models fades and jamming bursts.

## Known limitations of this proof of concept

- Each message is coded on its own. A burst that wipes out most of one short message's fragments still defeats the code; this shows up in the burst rows of the benchmark. Planned mitigations are interleaving across messages and adaptive parity driven by observed link quality.
- Emulation runs at the fragment level, not at the bit level, and assumes the radio's own link layer discards corrupted frames.
- No physical radios were used. Validation over real bearers is planned for a later phase.
