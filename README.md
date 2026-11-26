# Pharaoh Frosthold — أرض الصقيع الفرعونية

An Android survival-strategy and city-builder game built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Firebase (Firestore + Google Sign-In)**, blending ancient Egyptian Pharaonic civilization with industrial survival along a frozen Nile tundra.

لعبة بقاء واستراتيجية وإدارة مدن لأجهزة أندرويد، تجمع بين الحضارة الفرعونية والبقاء الصناعي في قلب صقيع النيل وعواصف الخماسين الجليدية، مع دعم كامل للغة العربية (RTL) والإنجليزية (LTR).

---

## Key Features — أبرز المميزات

### 1. Kingdom & Survival Simulation — إدارة المملكة والبقاء
* **Central Solar Obelisk (مسلّة الشمس):** Heats the settlement, features **Overdrive mode** (extra warmth at double coal consumption), and scales up to Lv.10.
* **10 Upgradeable 3D-Styled Facilities:**
  * *Production:* Hathor Coal Mine, Nile Timber Mill, Delta Hunters Guild, Pharaonic Iron Foundry.
  * *Survival:* Citizen Quarters, Royal Kitchen (3 meal quality tiers), Healing Temple.
  * *Research & Warfare:* Thoth Observatory, Sun Guard Barracks (Infantry, Lancers, Marksmen), Pharaonic Watchtower.
* **Day/Night & Khamaseen Blizzard Cycle:** Live camp temperature calculations, storm countdowns, animated flying resource gains/consumption, and offline resource calculation.
* **Living Citizens & Chief Orders:** Assignable workers with live Health, Warmth, Happiness, Hunger, and Fatigue stats, plus 3 active **Chief Orders** (*Royal Mobilization*, *Pharaoh's Double Feast*, *Frost Sham El-Nessim*).

### 2. Pharaonic Kings & Queens & Animated 2D Desert Battle Arena
* **8 Historical Royal Heroes with Custom 3D Portraits:**
  * **King Ramses II** (Epic Infantry) • **King Ahmose I** (Epic Marksman) • **Queen Nefertiti** (Mythic Lancer)
  * **King Djoser** (Rare Infantry) • **King Khufu** (Epic Marksman) • **Queen Hatshepsut** (Mythic Infantry)
  * **King Thutmose III** (Mythic Infantry) • **Queen Cleopatra VII** (Rare Marksman)
* **Hero Progression & Temple Gacha:** Level up with Hero XP, promote stars with shards, upgrade gear with iron, and summon via Silver & Golden Temple Keys.
* **Animated 2D Desert Battle Arena:** Deploy up to 5 heroes in dynamic real-time battles featuring idle breathing, attack lunges, hit recoil/flashes, directional Canvas projectiles/slashes/heal rays/ultimate novas, floating damage/heal numbers, and manual or auto Ultimate skills.

### 3. Living Nile World Map, Alliances & Player Citadels
* **Living Nile Tundra:** Animated river currents, drifting ice floes, swaying snow-dusted oasis palms, Giza pyramids, roaming beasts (*Frost Nile Croc*, *Glacial Anubis*, *Cryo Sphinx*), and resource deposits with animated dotted march trails.
* **Cloud Player Citadels & Rival Strongholds:** View other players' cloud-synced citadels and NPC alliance bastions (`[KMT]`, `[ANB]`, `[SOB]`) with Scout and Raid/March expeditions.
* **Alliance Hub:** Found an alliance (name, 2–5 character tag, description, banner color) or join/leave existing cloud alliances.

### 4. Real-Time Social, Profile, Shop & Leaderboards
* **Live Chat Ticker & World/Alliance Chat:** Persistent live ticker bar above the bottom dock opening a tabbed **World Chat / Alliance Chat** modal synced via Firestore.
* **Commander Profile:** Customize Chief Name, choose any of the 8 3D King/Queen avatars, select from 4 royal frames, and inspect kingdom career stats.
* **Royal Shop & Daily Gift:** Claim a **Daily Free Royal Gift** every 24 hours and spend Gems across 3 tabs (*Speedups & Keys*, *Resource Crates*, *Royal Bundles*).
* **3-Category Leaderboards:** Ranked by **Total Power**, **Development Score & Obelisk Level**, and **Exploration Stage**, showing each commander's avatar and alliance tag.
* **Chapter Missions, Daily Quests & Tech Tree:** 9 technologies across Economy, Growth, and Battle branches, multi-chapter story chests, and daily milestone chests.

---

## Tech Stack — التقنيات المستخدمة

* **Language & UI:** Kotlin, Jetpack Compose, Material Design 3 (`enableEdgeToEdge`, RTL/LTR adaptive layouts, `WindowSizeClass` NavigationRail support)
* **Architecture:** MVVM with `StateFlow`, Kotlin Coroutines, and Moshi JSON local persistence (`SaveRepository`)
* **Backend & Auth:** Firebase Authentication (Google Sign-In via Jetpack `CredentialManager`), Cloud Firestore real-time snapshot listeners, and zero-trust `firestore.rules`
* **Build:** Gradle Kotlin DSL (`minSdk 24`, `targetSdk 36`, `compileSdk 36.1`, Java 11)

---

## Project Structure — هيكل المشروع

```text
├── app/src/main/java/com/example/
│   ├── MainActivity.kt              # App host, adaptive dock/rail, modals & lifecycle sync
│   ├── audio/                       # Synthesized SFX engine
│   ├── auth/                        # Google Sign-In via CredentialManager & Firebase Auth flow
│   ├── data/                        # GameCatalog, SaveRepository (local + JSON), CloudRepository
│   ├── localization/                # Bilingual Arabic (RTL) & English (LTR) strings
│   ├── model/                       # GameModels (Moshi) & CloudModels (Firestore)
│   ├── ui/
│   │   ├── art/                     # AsyncDrawableImage, 3D portrait frames, vector illustrations
│   │   ├── components/              # TopHudBar, CampMap, Modals, Leaderboard, Shop, Chat, Alliances
│   │   ├── screens/                 # AuthScreen, HeroesScreen (2D Arena), WorldMapScreen, Tech/Quests
│   │   └── theme/                   # Royal Lapis, Warm Pharaoh Gold & Nile Turquoise M3 Theme
│   └── viewmodel/                   # GameViewModel simulation loop, combat engine & Firestore flows
├── firebase-blueprint.json          # Firestore data schema blueprint
├── firestore.rules                  # Zero-trust Firestore security rules
└── firestore.test.js                # Firestore Emulator security rules test suite
```

---

## Setup & Running — التشغيل والإعداد

### Prerequisites
* Android Studio (Ladybug / Meerkat or newer) with JDK 11+ and Android SDK 36
* A Firebase project with **Google Sign-In** enabled and **Cloud Firestore** provisioned

### Steps
1. **Clone the repository:**
   ```bash
   git clone <your-repo-url>
   cd <repo-folder>
   ```
2. **Firebase Configuration:**
   * Place your Firebase `google-services.json` inside `app/` and ensure `app/src/main/res/values/strings.xml` points `firestore_database_id` (and `default_web_client_id` via Google Services) to your Firebase project.
   * Deploy the security rules from `firestore.rules` to your Firestore database.
3. **Build & Run:**
   ```bash
   gradle :app:assembleDebug
   ```
   Or open the project in Android Studio and run the `app` configuration on an emulator or Android device (`minSdk 24+`).

### Testing Firestore Security Rules
With the Firebase Emulator Suite installed, you can verify all Firestore access-control rules locally:
```bash
firebase emulators:exec --only firestore "node --test firestore.test.js"
```
