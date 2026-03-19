# AGENTS Guide

## Project Snapshot
- Android app (`:app`) for volunteer scheduling against the `escala-voluntarios` Go backend.
- Stack: Kotlin + Compose + Navigation Compose + Hilt + Retrofit/Moshi + EncryptedSharedPreferences.
- Entry points: `app/src/main/java/br/com/leogsouza/escalav/EscalaApp.kt` and `app/src/main/java/br/com/leogsouza/escalav/MainActivity.kt`.

## Architecture You Should Preserve
- Single-activity + Compose navigation (`app/src/main/java/br/com/leogsouza/escalav/ui/AppNavigation.kt`).
- Feature folders under `ui/`: `auth/`, `schedule/`, `restrictions/`.
- Network boundary is `ApiService` (`app/src/main/java/br/com/leogsouza/escalav/data/remote/api/ApiService.kt`); ViewModels call it directly (no repository layer yet).
- App-wide DI lives in `app/src/main/java/br/com/leogsouza/escalav/di/AppModule.kt`.
- DTOs are Moshi data classes in `data/remote/dto/` with explicit `@Json` names for snake_case fields.

## End-to-End Data Flow
- Auth: `LoginScreen` -> `AuthViewModel.login()` -> `ApiService.login()` -> `TokenStore` save -> `AuthState.Success`.
- Session bootstrap: `AppNavigation` triggers `AuthViewModel.checkSession()` and picks start destination (`login` vs `calendar`).
- Auth header injection is centralized in `AppModule.provideOkHttp()` using `TokenStore.accessToken`.
- Calendar flow: `ScheduleViewModel` fetches active schedule then month events; UI reads `eventsByDate` map.
- Restriction CRUD (admin view): `RestrictionsListViewModel` + `RestrictionsFormViewModel` (split architecture). List shows ALL volunteers' restrictions with role filter chips, text search, and rich `RestrictionCard` components. Form uses `VolunteerSearchField` for volunteer selection, `ServiceCodeChips` from schedule events, `DayPatternSelector`, `SpecificDateSection`, and `DateRangeSection` for rules builder. Rules are serialized to `rules_json` JSON on save.

## Local Dev Workflow
- Required config is in root `local.properties`: `API_BASE_URL=http://10.0.2.2:8080` (or LAN IP for physical device).
- `BuildConfig.API_BASE_URL` is created in `app/build.gradle.kts`; networking appends `/` in Retrofit builder.
- README documents CLI build as `./gradlew assembleDebug`; Android Studio Run is the primary verified path in this repo snapshot.
- No test sources are currently present under `app/src/test` or `app/src/androidTest`.

## Project-Specific Conventions
- State pattern: each ViewModel exposes immutable `StateFlow` + private `MutableStateFlow` UI state classes.
- Screen side effects use `LaunchedEffect(...)` for initial loads/navigation (see `LoginScreen`, `DayDetailScreen`, `RestrictionFormScreen`).
- UI copy is Portuguese; keep new labels/messages aligned with existing language.
- Date keys are string-based (`yyyy-MM-dd`) and often derived with `substring(0, 10)` from API date values.
- Errors are surfaced as plain `String?` in state and rendered directly in composables.

## High-Impact Integration Notes
- Backend contract strongly shapes models: keep DTO names/`@Json` mappings consistent with API payloads.
- **Restrictions ViewModel split**: `RestrictionsListViewModel` owns list state (role filter, search, delete). `RestrictionsFormViewModel` owns form state (volunteer search, service codes, positions, rules parsing/serialization). These are intentionally separate — do NOT merge them back into a single ViewModel.
- `RestrictionFormScreen` gets schedule from `RestrictionsFormViewModel.formState.schedule`; volunteer from `formState.selectedVolunteer`. No dependency on list state.
- `AndroidManifest.xml` currently enables cleartext traffic for HTTP dev backends (`android:usesCleartextTraffic="true"`).

## Safe Change Strategy For Agents
- For API changes: update DTO + `ApiService` + affected ViewModel state mapping + composable rendering in one patch.
- For new screens: add `Screen` route object + `NavHost` entry in `AppNavigation.kt`, then follow existing `hiltViewModel()` pattern.
- Prefer incremental refactors (introducing repositories/use-cases) behind existing ViewModel interfaces to avoid navigation/state regressions.
- Shared restriction UI components live in `ui/restrictions/components/`: `ScheduleBanner`, `RestrictionCard`, `RulesSummary`, `VolunteerSearchField`, `ServiceCodeChips`, `DayPatternSelector`, `SpecificDateSection`, `DateRangeSection`.
- Service codes for the form come from `GET /schedules/:id/service-codes` — this endpoint returns distinct codes for events in a given schedule.
- Rules JSON format: `{"mode":"exclude","operator":"OR","serviceCodes":[...],"dayPattern":"even","specificDates":[{"date":"...","positionID":null,"notes":""}],"dateRanges":[{"start":"...","end":"...","positionID":null}]}`.
- Weekdays and periods fields exist in the backend DTO but are intentionally hidden in the mobile UI.

