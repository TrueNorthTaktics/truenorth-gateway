# ATAK plugin (next stage)

This Android module will host `core/` unchanged inside an ATAK plugin. It is not built yet, because it needs the ATAK plugin SDK.

## Planned features

- **Report forms generated from message definitions.** Loading a new definition adds a new form.
- **Draft from voice or chat.** Offline speech-to-text, in English and French, pre-fills a report. The operator always confirms before it is sent.
- **Send by link.** The plugin picks the format and FEC level for the active bearer and shows what was sent and what has been acknowledged.
- **Inbox.** Messages arriving in any supported format are decoded and shown on the map as CoT.
- **Evacuation-options view.** Status messages from land, air and maritime assets are fused into "who can get them out, and how fast".
