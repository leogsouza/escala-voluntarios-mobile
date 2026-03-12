# Escala de Voluntários — Mobile

Android mobile app for the **Escala de Voluntários** volunteer scheduling system used by CCB church services. Built with Expo (SDK 54) and React Native.

---

## Features

- **Authentication** — Login with JWT, biometric lock (fingerprint/face) on app resume
- **Schedule Calendar** — Monthly calendar view showing service days with color-coded dots per service type
- **Day Detail** — List of events for a selected day with volunteer assignment cards
- **Event Detail** — Full event details and volunteer assignments
- **Restrictions** — Create, edit, and delete personal availability restrictions (exclude/include rules by date, weekday, date range, or service period)
- **Offline Support** — MMKV-persisted React Query cache with a network banner when offline

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | [Expo](https://expo.dev) SDK 54 + React Native 0.81 |
| Navigation | [Expo Router](https://expo.github.io/router) v6 (file-based) |
| State / Data | [TanStack React Query](https://tanstack.com/query) v5 |
| Persistence | [react-native-mmkv](https://github.com/mrousavy/react-native-mmkv) |
| Auth storage | [expo-secure-store](https://docs.expo.dev/versions/latest/sdk/securestore/) |
| Biometrics | [expo-local-authentication](https://docs.expo.dev/versions/latest/sdk/local-authentication/) |
| UI components | [react-native-paper](https://reactnativepaper.com/) v5 |
| Calendar | [react-native-calendars](https://github.com/wix/react-native-calendars) |
| Date handling | [dayjs](https://day.js.org/) |
| Language | TypeScript 5 |

---

## Project Structure

```
src/
  app/                      # Expo Router file-based routes
    (auth)/login.tsx         # Login screen
    (tabs)/
      schedule/             # Calendar, day detail, event detail
      restrictions/         # Restriction list, create, edit
  components/               # Reusable UI components
  hooks/queries/            # TanStack Query hooks
  services/                 # API fetch functions
  lib/                      # Auth context, query client, MMKV persister
  types/                    # TypeScript interfaces
  constants/                # API base URL, brand colors
```

---

## Getting Started

### Prerequisites

- Node.js 18+
- [Expo CLI](https://docs.expo.dev/get-started/installation/) (`npm install -g expo-cli`)
- [Expo Go](https://expo.dev/client) app (SDK 54) on your Android device
- The [escala-voluntarios](https://github.com/leogsouza/escala-voluntarios) backend running

### Installation

```bash
git clone https://github.com/leogsouza/escala-voluntarios-mobile.git
cd escala-voluntarios-mobile
npm install
```

### Environment

Create a `.env` file at the project root:

```env
EXPO_PUBLIC_API_BASE_URL=http://<your-backend-host>:8080
```

> If omitted, defaults to `http://localhost:8080`.

### Running on Device (Expo Go)

```bash
npx expo start --tunnel
```

Scan the QR code with the Expo Go app on your Android phone.

### Running on Emulator

```bash
npx expo start
# Press 'a' to open Android emulator
```

---

## Available Scripts

| Script | Description |
|---|---|
| `npm start` | Start Expo dev server |
| `npm run android` | Build and run on Android device/emulator |
| `npm test` | Run Jest test suite |
| `npm run typecheck` | TypeScript type check (no emit) |
| `npm run lint` | ESLint |

---

## Testing

```bash
npm test
```

- 19 test suites, 84 tests
- Uses `jest-expo` preset with `@testing-library/react-native`

---

## Backend

This app connects to the [escala-voluntarios](https://github.com/leogsouza/escala-voluntarios) Go backend (Fiber + GORM + MySQL). See that repository for setup instructions.

---

## License

Private — CCB internal use only.
