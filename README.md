<div align="center">

<img src="app/src/main/res/drawable-nodpi/ic_oblivion_logo.png" width="140" alt="Oblivion logo">

# Oblivion V2

**Anti-forensic duress-wipe system for Android**

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-red.svg)](LICENSE)
[![Android: 8.0+](https://img.shields.io/badge/Android-8.0%20→%2014+-green.svg)]()
[![Offline](https://img.shields.io/badge/100%25-offline-blue.svg)]()
[![Telemetry: 0](https://img.shields.io/badge/Telemetry-0-black.svg)]()

*When someone gets their hands on your phone, you decide what stays — and what disappears.*

[Documentation française](docs/README.fr.md) · [Security policy](SECURITY.md) · [Known limitations](#known-limitations)

</div>

---

## Intended audience

Oblivion is a defensive anti-forensic system designed to protect personal data
against unlawful seizure. It is built for **users who face real risk to their
data sovereignty**, including:

- Journalists operating in conflict zones or under authoritarian regimes
- Human rights activists, dissidents, and political organizers
- Whistleblowers protecting source identity
- Survivors of domestic violence and stalking
- Security researchers studying duress-resistance systems
- Privacy-conscious individuals who need provable data sovereignty

This software is provided under the **GNU Affero General Public License v3.0**.
Misuse for the destruction of evidence in lawful criminal proceedings is
illegal under most jurisdictions and is not endorsed by the maintainers.

---

## How it works

Oblivion provides **seven independent triggers**. Each can be activated and
configured separately. Any one of them, when fired, will execute a full
factory reset via the native Android `DevicePolicyManager.wipeData()` API.

| # | Trigger | Mechanism |
|---|---|---|
| 1 | **Guard — Lockscreen** | Distress PIN, length-trap, or N failed attempts (via `AccessibilityService`) |
| 2 | **USB Kill** | USB/charger connection while locked → countdown → wipe |
| 3 | **SMS Wipe** | Authorized number + secret keyword via `BroadcastReceiver` |
| 4 | **Voice Wipe** | Offline keyphrase recognition (Vosk · FR + EN models bundled) |
| 5 | **Dead Man's Switch** | Auto-wipe if no biometric check-in within N hours/days |
| 6 | **Scheduled Wipe** | Exact `AlarmManager`, reboot-resistant |
| 7 | **Decoy Mode** | Decoy PIN → fake "System Update" full-screen page while wipe runs silently behind |

All triggers run in parallel. Disarming any trigger requires biometric
authentication. The application has **no `INTERNET` permission** declared and
is technically incapable of network communication.

---

## Security stack

| Component | Implementation |
|---|---|
| Encryption | AES-256-GCM via `EncryptedSharedPreferences` |
| Master key | Hardware-backed Android Keystore |
| PIN hash | PBKDF2-HMAC-SHA256, 100 000 iterations + 32-byte random salt |
| PIN comparison | Timing-safe (side-channel resistant) |
| Wipe mechanism | `DevicePolicyManager.wipeData()` (native Device Admin) |
| Voice recognition | Vosk (offline, FR + EN small models bundled) |
| Persistence | `WorkManager` + `AlarmManager` exact, reboot-resistant |
| Min SDK | API 26 · Android 8.0 |
| Target SDK | API 33 · intentional (API 34+ blocks `wipeData()` for non-DO apps) |

## Privacy guarantees

- No data is ever sent to a third-party server
- Zero telemetry, zero analytics, no crash reporters
- All secrets stored in `EncryptedSharedPreferences` (Tink/AES-256-GCM)
- No persistent notifications at runtime (the decoy notification is intentional)
- All triggers can be individually toggled and configured
- Biometric authentication required to disarm any trigger
- Full persistence across reboot and force-stop
- Open-source, auditable, modifiable

---

## Build from source

### Prerequisites

- Android Studio **Hedgehog (2023.1.1)** or newer
- JDK 17 (bundled with Android Studio)
- Android SDK 34 installed
- A device or emulator running **Android 10 or newer**

### Build commands

```bash
git clone https://github.com/lethe-labs/oblivion-v2.git
cd oblivion-v2
./gradlew assembleRelease
```

Output APK: `app/build/outputs/apk/release/app-release.apk`

Unit tests (pure JVM, no device required):

```bash
./gradlew testDebugUnitTest
```

### Signing your own release build

Create a `keystore.properties` file at the project root:

```properties
storeFile=path/to/your.keystore
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

This file is in `.gitignore` and **must never be committed**.

### Vosk voice models

The repository ships with both the **French** and the **English small models**
in `app/src/main/assets/model-fr/` and `app/src/main/assets/model-en/`
(~65 MB each). Nothing to download for a standard build.

To replace a model with a different one, download it from
<https://alphacephei.com/vosk/models>:

- **FR**: `vosk-model-small-fr-pguyot`
- **EN**: `vosk-model-small-en-us-0.15`

Extract the ZIP and copy its contents into `app/src/main/assets/model-<lang>/`,
matching the structure of the models already present.

Both models are released under **Apache 2.0** — commercial use is permitted.

---

## Setup on device

1. Install the signed APK (accept "Install from unknown sources" prompt)
2. Open Oblivion, set the master PIN
3. **Enable Device Admin**: Settings → Security → Device admin apps → Oblivion
4. **Enable Accessibility Service** (only if using Guard): Settings → Accessibility → Oblivion
5. Grant the runtime permissions matching your chosen triggers
6. Configure the triggers you want and arm them

Total setup time: ~5 minutes.

---

## Known limitations

Oblivion is built on standard Android APIs and respects the platform's
constraints. The following limitations are known and documented:

- **External SD card is not wiped.** `DevicePolicyManager.wipeData()` only
  factory-resets internal storage. If your threat model includes external
  storage, encrypt it separately.
- **No protection against hot RAM extraction** on devices that are seized
  while powered-on and unlocked. The wipe requires a triggered event.
- **No protection against post-wipe forensic recovery** on unencrypted
  devices or those with broken File-Based Encryption. Modern Android 10+
  with FBE active makes recovery extremely difficult.
- **The app icon remains visible** in the launcher. Use a custom launcher
  to hide it if your threat model requires concealment.
- **Voice recognition can fire accidentally** if the keyphrase is too
  common. Choose 3–5 unusual words together. False-positive risk is real.
- **WorkManager periodic minimum is 15 minutes.** The Dead Man's Switch
  checks every 15 minutes; expect up to that delay between expiry and wipe.
- **SMS sender identity can be spoofed.** Caller ID on an inbound SMS is not
  authenticated by the network and can be forged through commercial gateways.
  Anyone who learns your keyword can therefore fire the SMS trigger remotely.
  No receiver-side check can prevent this: treat the keyword as a secret of
  the same value as the duress PIN, and disable the SMS trigger if remote
  wipe is not worth that risk. Configuring the authorised number in full
  international form (`+33…`) is stricter than the national form.
- **Choose a duress PIN that is not a suffix of your real PIN.** The
  lockscreen triggers fire as soon as the last digits you typed match the
  duress secret — that is what lets them work without pressing "validate",
  and what lets them still work after a mistyped attempt. As a consequence a
  real PIN of `981234` would trigger a duress PIN of `1234` on every normal
  unlock.

---

## Threat model

Oblivion is designed to be effective against:

- ✓ Unlawful device seizure (search, theft, coercion)
- ✓ Offline forensic acquisition attempts (USB, JTAG via Cellebrite, GrayKey)
- ✓ PIN-brute-force and shoulder-surfing attacks
- ✓ Remote compromise scenarios where SMS authority is preserved

Oblivion is **not** designed to defend against:

- ✗ State-actor-level adversaries with kernel-level exploits or hardware modification
- ✗ Devices already unlocked and live in the adversary's hands
- ✗ Pre-wipe network exfiltration by malware already installed
- ✗ Lawful seizure where you are legally compelled to provide access

---

## Roadmap

Future trigger candidates under consideration:

- Shake / panic gesture trigger
- Volume button combo trigger
- Wi-Fi network-based smart logic (reduce false positives)
- Geofence-based arm/disarm
- NFC tag disarm
- Stealth launcher (hide app icon)
- Selective wipe (apps + photos instead of factory reset)
- Multi-decoy modes (low battery, no signal, etc.)
- Camera trap (front-camera photo on failed lockscreen attempt)
- Pre-wipe encrypted "last message" via SMS

Pull requests are welcome. Please open an issue first for discussion.

---

## Contributing

This project welcomes contributions from the privacy and security community.
Please open an issue for discussion before submitting larger pull requests.

By contributing, you agree to license your contribution under the same
license as the project (AGPL-3.0-or-later).

For security issues, please follow the [security disclosure policy](SECURITY.md)
rather than opening a public issue.

---

## License

This project is licensed under the **GNU Affero General Public License v3.0
or later** — see [LICENSE](LICENSE) for the full text.

The AGPL ensures that any modified version of Oblivion, even when distributed
over a network as a service, must remain free and open. This protects against
proprietary forks that would erode trust in the underlying tool.

```
Oblivion V2 — Anti-forensic duress-wipe system for Android
Copyright (C) 2025–present  lethe-labs

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.
```

---

## Contact

- **GitHub**: <https://github.com/lethe-labs>
- **Security**: see [SECURITY.md](SECURITY.md) for the PGP key and disclosure policy
- **F-Droid**: submission planned

---

<div align="center">

*Aucune donnée ne quitte jamais l'appareil.*<br>
*No data ever leaves the device.*

</div>
