# 🚨 Disaster SOS Pakistan

> **Offline Emergency Response Android App for Pakistan**  
> Peer-to-peer mesh networking for life-saving communication when cellular networks fail

![Status](https://img.shields.io/badge/status-POC%20Complete-success)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Language](https://img.shields.io/badge/language-Kotlin-purple)
![API](https://img.shields.io/badge/API-26%2B-blue)

---

## 📖 About

**Disaster SOS** is an Android application designed to save lives during natural disasters in Pakistan — earthquakes, floods, and cyclones — when traditional communication infrastructure (cell towers, internet) fails.

The app enables **peer-to-peer (P2P) mesh communication** between nearby Android devices using Bluetooth and WiFi Direct, allowing users to broadcast SOS signals with GPS coordinates to anyone within range, even when completely offline.

Built as a response to Pakistan's 2022 floods, which displaced 33 million people and knocked out communication infrastructure across Sindh, Balochistan, and KPK.

---

## ✅ Current Status: Proof of Concept (POC) Complete

The core mesh networking has been validated on real devices:

- ✅ Two physical Android phones (Realme RMX3830 & RMX3231) successfully discovered each other
- ✅ GPS coordinates transmitted peer-to-peer without internet
- ✅ SOS broadcasting confirmed working at ±12m accuracy
- ✅ Verified fully offline operation (airplane mode + Bluetooth/WiFi only)

---

## 🎯 Core Features (Planned)

### Phase 1 — POC ✅ Complete
- [x] Device-to-device discovery via Nearby Connections API
- [x] GPS location sharing
- [x] SOS broadcasting between phones
- [x] Clean Jetpack Compose UI with Material 3

### Phase 2 — In Progress 🚧
- [ ] Offline map integration (osmdroid + OpenStreetMap)
- [ ] Visual SOS pins on pre-downloaded Pakistan maps
- [ ] Shelter and hospital database

### Phase 3 — Planned 📋
- [ ] Encrypted local database (Room + SQLCipher)
- [ ] First-aid guide (Urdu, Sindhi, English)
- [ ] Family "I'm Alive" status broadcasting
- [ ] Community hazard reporting
- [ ] NDMA alert integration
- [ ] Multi-language support (Urdu, Sindhi, Pashto)

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **P2P Communication** | Google Nearby Connections API |
| **Offline Maps** | osmdroid + OpenStreetMap |
| **Database** | Room + SQLCipher (encrypted) |
| **Security** | Android Keystore + EncryptedSharedPreferences |
| **Build System** | Gradle (Kotlin DSL) |
| **Min SDK** | API 26 (Android 8.0) |
| **Target SDK** | API 35 (Android 15) |

---

## 🔒 Security & Privacy

- **AES-256 encryption** for all stored data
- **Hardware-backed keys** via Android Keystore
- **End-to-end encryption** for P2P messages
- **No data collection** — all core features work 100% offline
- **OWASP MASVS** aligned security architecture
- **Biometric app lock** for sensitive features

---

## ♿ Accessibility & HCI

Designed with **Human-Computer Interaction** principles for crisis situations:

- **Fitts' Law** — One-tap SOS with large touch targets (56dp+)
- **Hick's Law** — Maximum 3 actions to any critical feature
- **WCAG 2.2 Level AA** compliance
- **Full TalkBack** screen reader support
- **RTL layout** support for Urdu, Sindhi, Pashto
- **High contrast** mode for emergency situations
- **Multi-sensory feedback** (visual + audio + haptic)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1) or later
- JDK 17
- Android device running Android 8.0+ (API 26+)
- **2 physical devices** required for testing mesh features

### Build & Run
```bash
# Clone the repository
git clone https://github.com/awais-ali-dev/disaster-sos-pakistan.git

# Open in Android Studio
# Wait for Gradle sync to complete
# Connect an Android device and click Run ▶️
```

### Testing the Mesh Feature
1. Install the app on **two Android phones**
2. Enable **Bluetooth + WiFi** on both devices
3. Grant all requested permissions
4. Open the app on both phones
5. Tap **"Start Mesh"** on both
6. Tap the **red SOS button** on one phone
7. Verify the other phone receives the SOS with GPS coordinates

---

## 📸 Screenshots

*Coming soon — POC successfully tested on Realme RMX3830 & RMX3231*

---

## 👨‍💻 Developer

**Awais Ali**  
BSCS Student | Sukkur IBA University  
Pakistan 🇵🇰

📧 Contact: awaisali.bscsf22@iba-suk.edu.pk  
🎓 Research Focus: Deep Learning in Medical Imaging, Mobile Applications for Social Good

---

## 🤝 Contributing

This is currently a solo academic project, but contributions are welcome once the core MVP is complete. Feel free to open an issue for suggestions or bug reports.

---

## 📜 License

Copyright © 2026 Awais Ali. All rights reserved.

*License to be finalized before public release.*

---

## 🙏 Acknowledgments

- **NDMA Pakistan** — for raising awareness about disaster preparedness needs
- **OpenStreetMap community** — for free, open map data
- **Google Nearby Connections team** — for making offline P2P possible
- **Sukkur IBA University** — for academic support

---

## 📚 References

- [Google Nearby Connections API](https://developers.google.com/nearby/connections/overview)
- [osmdroid — OpenStreetMap for Android](https://github.com/osmdroid/osmdroid)
- [NDMA Pakistan](https://www.ndma.gov.pk/)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)

---

<p align="center">
  <b>Built with ❤️ for Pakistan | Technology that saves lives</b>
</p>
