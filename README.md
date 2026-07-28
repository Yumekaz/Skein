# Skein

**Offline-first Bluetooth mesh messaging for privacy, campus, and disaster-resilient communications.**

> [!WARNING]
> This software has not received external security review and may contain vulnerabilities. Do not rely on it for high-stakes or sensitive use cases until it has been independently audited. Work in progress.

Skein is a decentralized, peer-to-peer messaging app for Android. It forms a multi-hop mesh over Bluetooth Low Energy so people can chat without internet, phone numbers, or central servers. Optional internet features (geohash channels via Nostr, Tor routing) extend reach when connectivity is available.

Skein is based on the open-source [bitchat-android](https://github.com/permissionlesstech/bitchat-android) mesh stack and is evolved here as a major academic/engineering project: same foundational transport, encryption, and relay ideas, with a distinct product identity and a roadmap focused on reliability under loss (e.g. causal ordering and FEC as planned research extensions).

| | |
|---|---|
| **Platform** | Android 8.0+ (API 26) |
| **applicationId** | `com.skein.android` |
| **Package** | `com.skein.android` |
| **UI** | Jetpack Compose, Material 3 |
| **License** | GNU GPL v3 — see [LICENSE.md](LICENSE.md) |

---

## Features

- **Bluetooth LE mesh** — Automatic peer discovery and multi-hop relay without infrastructure
- **End-to-end encryption** — X25519 key exchange + AES-256-GCM for private messages; Noise-based secure sessions
- **Channels** — Topic-based group chat with optional password protection
- **Store-and-forward** — Messages cached for offline peers and delivered when they reconnect
- **File & media transfer** — Images, voice notes, and generic files over the mesh (fragmented for BLE MTU)
- **Gossip sync** — Eventual consistency of public packets via compact GCS filters
- **Source routing (v2)** — Explicit multi-hop paths for efficient unicast when topology is known
- **Privacy-first identity** — No accounts, no phone numbers; local nicknames and keys only
- **IRC-style commands** — `/join`, `/msg`, `/who`, and related chat controls
- **Emergency wipe** — Triple-tap branding control to clear local data
- **Optional geohash / Nostr mode** — Location-scoped channels over the internet when enabled
- **Bundled Tor (Arti)** — Optional anonymous routing for internet-facing features
- **Battery-aware BLE** — Adaptive scan duty cycle and power modes

### Roadmap (research)

Reliability extensions implemented for Skein include deterministic Lamport ordering for mesh messages and opt-in Reed–Solomon FEC for direct BLE message/file transfers between matching-capability peers. FEC stays off for legacy/iOS peers and control traffic; it uses 8 data plus 4 parity shards per bounded block, supports multi-block v2 packet frames up to 1 MiB, and falls back to normal fragmentation whenever capability is absent or a transfer is outside the bounded FEC path. This remains an engineering feature, not a claim of production security review.

---

## Build & run

### Prerequisites

- Android Studio (recent stable recommended)
- Android SDK API 26+
- JDK suitable for the project’s Gradle toolchain
- A physical device with Bluetooth LE (emulators have limited BLE support)

### Clone and build

```bash
git clone https://github.com/Yumekaz/Skein.git
cd Skein

# Full build
./gradlew build

# Debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

Debug APK output path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release:

```bash
./gradlew assembleRelease
# or App Bundle:
./gradlew bundleRelease
```

Windows: use `gradlew.bat` with the same tasks.

### applicationId

The Android application ID is **`com.skein.android`**. Use that ID for sideloading, store listings, and deep-link configuration.

---

## Permissions & hardware

| Permission / capability | Why |
|-------------------------|-----|
| Bluetooth (scan / connect / advertise) | Mesh discovery and messaging |
| Location | Required by Android for BLE scanning on many API levels; also used locally for optional geohash channels |
| Notifications | Message alerts while the mesh service runs |
| Microphone (optional) | Voice notes |
| Network (optional) | Geohash/Nostr and Tor-backed internet features |

**Hardware:** Bluetooth LE required; Android 8.0+; ~2 GB RAM recommended.

Background mesh operation is tied to a foreground service (`MeshForegroundService`) so scanning and advertising can continue under Android background limits.

---

## Usage

1. Install a debug or release build on Android 8.0+.
2. Grant Bluetooth and location permissions when prompted.
3. Launch Skein — mesh networking starts with the app/service lifecycle.
4. Set a nickname (or keep the generated one).
5. Chat in the public mesh, join a channel, or open a private conversation when peers are nearby.

### Useful commands

| Command | Action |
|---------|--------|
| `/j #channel` | Join or create a channel |
| `/m @name message` | Private message |
| `/w` | List online users |
| `/channels` | List discovered channels |
| `/block @name` | Block a peer |
| `/unblock @name` | Unblock a peer |
| `/clear` | Clear chat messages |
| `/pass [password]` | Set channel password (owner) |
| `/transfer @name` | Transfer channel ownership |
| `/save` | Toggle channel message retention (owner) |

---

## Security & privacy (summary)

- Private messages: X25519 + AES-256-GCM; channel passwords via Argon2id + AES-256-GCM
- Signatures: Ed25519 for authenticity where applied
- No developer backend for mesh chat — no accounts, no central message store
- Keys, nicknames, favorites, and optional retained history stay on-device
- Location is processed locally for BLE requirements and optional geohash; precise coordinates are not uploaded by the app to a Skein server (there is none)
- See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for the full policy

---

## Architecture (high level)

| Area | Role |
|------|------|
| `mesh/` | BLE discovery, GATT client/server, relay, store-forward |
| `protocol/` | Binary wire format, compression, padding |
| `noise/` / `crypto/` | Secure sessions and crypto primitives |
| `service/` | Foreground mesh service and lifecycle |
| `ui/` | Compose screens and ViewModels |
| `nostr/` / `geohash/` | Optional internet location channels |
| `net/` | Tor/Arti and HTTP helpers |
| `features/` | Voice, file, media helpers |

Stack: Kotlin, Coroutines/Flow, Jetpack Compose, Nordic BLE helpers, BouncyCastle, optional Arti native libs. Technical write-ups live under [`docs/`](docs/).

For AI/agent contributors, see [AGENTS.md](AGENTS.md).

---

## Performance notes

- LZ4 compression for larger text payloads
- Adaptive BLE duty cycle from battery level
- Fragmentation for large file transfers over constrained MTUs
- Local-only gossip sync to converge public mesh state without flooding the wide area

---

## Contributing

Contributions that improve reliability, battery behavior, protocol clarity, tests, and documentation are welcome. Please:

1. Keep mesh operations off the main thread (coroutines / suspend).
2. Treat BLE and permissions defensively — hardware and OEM behavior varies.
3. Open issues with device model, Android version, and logs when reporting bugs.

Security-sensitive reports should be handled privately when possible rather than in public issues with exploit detail.

---

## Attribution

Skein builds on the open-source bitchat Android mesh implementation and related protocol work from the broader offline-messaging community. This repository rebrands and extends that foundation for research and product development under the Skein name. Upstream ideas, wire formats, and battle-tested BLE mesh patterns are gratefully acknowledged; Skein does not claim to have invented Bluetooth mesh messaging from scratch.

---

## License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE.md](LICENSE.md).
