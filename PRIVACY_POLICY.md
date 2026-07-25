# Skein Privacy Policy

*Last updated: July 2026*

## Purpose of this document

Skein is an offline-first messaging application for Android. It is designed so that everyday mesh chat does not depend on a developer-operated backend. This policy describes what the app stores on your device, what is shared with other users or optional third-party infrastructure, and what the maintainers of this open-source project do **not** collect.

This policy applies to the Skein Android application (`com.skein.android`) as distributed from this project’s source tree or builds you produce from it.

## Short summary

- **No Skein developer backend** collects your messages, contacts, or account data — the project does not run a central user database for mesh chat.
- **Mesh mode** works peer-to-peer over Bluetooth Low Energy, including multi-hop relay among nearby devices, without internet.
- **Optional internet features** (geohash channels over Nostr, Tor routing) only apply when those modes are used and connectivity is available.
- **Open source** — you can inspect the code to verify local storage and networking behavior.

## Modes of communication

### Bluetooth mesh (primary)

Devices discover each other over Bluetooth LE and exchange encrypted or public messages according to the mesh protocol. Traffic stays on the local radio mesh. There is no Skein server in this path.

### Optional geohash / Nostr (internet)

If you use location-scoped channels that rely on the internet, the app may connect to **decentralized Nostr relays** (not a proprietary Skein cloud). Your approximate region may be represented as a **geohash** (a coarse grid cell), not as continuous raw GPS tracks sent to project maintainers.

### Optional Tor

Where enabled, some internet-facing traffic may be routed through Tor via a bundled Arti-based stack to reduce network-level exposure. Tor exit and relay operators are independent of the Skein project.

## Data stored on your device

Skein may store the following **locally** (exact mechanisms include app private storage and encrypted preferences where implemented):

| Data | Purpose |
|------|---------|
| Cryptographic identity material | Session and long-lived keys needed to authenticate and encrypt |
| Nickname | Display name shown to peers |
| Favorites / peer bookkeeping | Recognize peers you marked across launches |
| Message history | Ephemeral in-session history; longer retention only if channel retention features are enabled |
| App settings | UI, power, Tor, and similar preferences |
| Received media files | Voice notes, images, or files you accept, under app-controlled paths |

You can remove local data by clearing app storage, uninstalling the app, or using in-app emergency wipe (where available).

### Temporary runtime state

While running, the app may hold in memory:

- Active Bluetooth connections and routing hints
- Recent packets for deduplication and store-and-forward
- Sync filters and short-lived caches for mesh gossip

This state is not uploaded to a Skein analytics or messaging server.

## What other people can see

### Nearby or mesh peers

Depending on what you send and which rooms you join, peers may observe:

- Your nickname
- Public keys or ephemeral identifiers used by the protocol
- Content of public or channel messages you send
- Content of private messages **only** if they are the intended recipient (end-to-end encryption applies to private messaging paths)
- Coarse radio signal quality indicators used for connectivity (not a street address)

### Channel members

In password-protected or topic channels, other members with access can see your nickname and messages in that channel, subject to the channel’s encryption and membership rules.

### Nostr relays (optional mode only)

If you use geohash/Nostr features, relays you connect to may see protocol events you publish (including coarse geohash tags and public keys used for those channels). Relay operators are third parties. Prefer mesh-only use if you do not want any internet-facing metadata.

## What this project does not do

Maintainers of Skein, as described by this codebase’s design:

- Do **not** require account registration, email, or phone number
- Do **not** operate a central store of your chat history
- Do **not** ship product analytics or advertising SDKs as part of the core mesh design described here
- Do **not** sell personal data
- Do **not** collect a continuous location history on behalf of the project

If you install a **modified** build from a third party, that build may behave differently. Only trust binaries and source you can verify.

## Encryption (technical overview)

Private messaging uses modern public-key and symmetric cryptography (including X25519 key agreement, AES-256-GCM for payload confidentiality, and Ed25519 signatures where the protocol applies them). Password-protected channels use password-based key derivation (Argon2id) with symmetric encryption. Implementation details live in the open-source tree under packages such as `crypto/`, `noise/`, and related modules.

No encryption scheme is perfect. The project has not claimed a formal external audit for every build; treat threat models accordingly.

## Location permission

Android may require **location permission** for Bluetooth LE scanning on many versions of the OS. Skein uses that permission:

1. **For BLE mesh discovery** — to satisfy platform APIs so nearby peers can be found. The app does not use this as a justification to upload your location to a Skein server.
2. **For optional geohash channels** — to compute a coarse geohash on-device when that feature is active. Precise coordinates are not the unit of sharing; the geohash cell is.

You can limit optional location features by not using geohash/internet modes; mesh still depends on whatever permissions Android requires for BLE.

## Children

Skein does not knowingly collect personal information from children through a developer backend. Because the app does not create central accounts, there is no project-side child profile system. Guardians should still supervise device and network use as appropriate.

## Data retention

| Category | Typical lifetime |
|----------|------------------|
| In-memory chats | Cleared when the process ends or you clear the UI, unless retention features keep local copies |
| Identity keys | Until you wipe data or uninstall |
| Favorites | Until you remove them or wipe data |
| Mesh caches | Hours-scale or session-bound, per protocol design |
| Nostr relay copies | Controlled by each relay’s policy (outside this app) |

## Your control

- Stop using mesh or internet features at any time
- Revoke permissions in system settings
- Clear storage or uninstall to remove local state
- Use emergency wipe if the build exposes it
- Audit or fork the source under the project license

## Changes to this policy

Updates will revise the “Last updated” date. Because mesh chat does not rely on a central user database, policy changes do not retroactively alter data the project never collected. Optional third-party relays may have their own policies.

## Contact

Skein is an open-source project. For privacy questions, security concerns, or documentation fixes:

- Review the source: https://github.com/Yumekaz/Skein
- Open a **repository issue**: https://github.com/Yumekaz/Skein/issues

Do not send sensitive exploit details in public issues if a private channel is available.

## Closing note

Skein’s design goal is resilient local communication: campus meshes, field use, and privacy-preserving chat without mandatory cloud identity. Privacy depends on correct app behavior, peer honesty in public channels, platform permissions, and — when internet features are enabled — the relays and networks you touch. Read the code, minimize optional internet modes when you need maximum locality, and treat all messaging tools with appropriate operational care.
