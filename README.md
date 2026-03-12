# Escala Voluntários — Android

Native Android app for the **Escala de Voluntários** volunteer scheduling system used by CCB church services.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Network | Retrofit 2 + Moshi |
| DI | Hilt |
| Auth storage | EncryptedSharedPreferences |

---

## Screens

- **Login** — JWT authentication
- **Calendar** — Monthly view with event dots per day
- **Day Detail** — List of events for a selected day
- **Event Detail** — Event info and volunteer assignment cards
- **Restrictions** — List, create, edit, and delete personal availability restrictions

---

## Setup

### Prerequisites

- Android Studio Hedgehog 2023.1 or newer
- Android SDK 34
- The [escala-voluntarios](https://github.com/leogsouza/escala-voluntarios) Go backend running

### Configuration

Create `local.properties` at project root (already gitignored):

```properties
sdk.dir=/path/to/your/Android/Sdk
API_BASE_URL=http://10.0.2.2:8080
```

> `10.0.2.2` is the Android emulator's loopback alias for your host machine's `localhost`.
> For a physical device on the same LAN, use your machine's local IP (e.g. `http://192.168.1.x:8080`).

### Build & Run

Open the project in Android Studio and click **Run**, or build from the command line:

```bash
./gradlew assembleDebug
```

---

## Backend

This app connects to the [escala-voluntarios](https://github.com/leogsouza/escala-voluntarios) Go backend (Fiber + GORM + MySQL). See that repository for setup instructions.

---

## License

Private — CCB internal use only.
