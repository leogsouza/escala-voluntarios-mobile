# Restriction Feature Redesign — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rewrite the mobile restriction list + form to reach full feature parity with the web app, including service code selection, day patterns, specific dates with positions, date ranges, admin volunteer search, role filtering, and proper rules_json assembly.

**Architecture:** Two independent ViewModels (list + form) replacing the broken shared ViewModel. New backend endpoint for schedule-scoped service codes. Rich composable components matching EventDetailScreen's visual quality. Form assembles structured rules into JSON matching the web's format.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt DI, Retrofit/Moshi, Go (Fiber/GORM) backend

**Design Doc:** `docs/plans/2026-03-19-restriction-redesign-design.md`

---

## Task 1: Backend — Schedule Service Codes Endpoint

**Goal:** Create `GET /schedules/:id/service-codes` that returns distinct service codes used by events in a schedule.

**Files:**
- Modify: `backend/internal/repositories/schedule_repository.go`
- Modify: `backend/internal/services/schedule/schedule_service.go`
- Modify: `backend/internal/handlers/schedule_handler.go`
- Modify: `backend/internal/handlers/routes.go`

**Step 1: Add repository method**

In `schedule_repository.go`, add to the `ScheduleRepository` interface and implementation:

```go
// Interface
GetServiceCodesForSchedule(scheduleID uint64, churchID uint64) ([]models.ServiceCode, error)

// Implementation
func (r *scheduleRepository) GetServiceCodesForSchedule(scheduleID uint64, churchID uint64) ([]models.ServiceCode, error) {
    var codes []models.ServiceCode
    err := r.db.
        Distinct("service_codes.*").
        Table("service_codes").
        Joins("JOIN events ON events.service_code = service_codes.code").
        Where("events.schedule_id = ? AND events.church_id = ?", scheduleID, churchID).
        Order("service_codes.display_order").
        Find(&codes).Error
    return codes, err
}
```

**Step 2: Add service method**

In `schedule_service.go`, add to the `Service` interface and implementation:

```go
// Interface
GetServiceCodesForSchedule(scheduleID uint64, churchID uint64) ([]models.ServiceCode, error)

// Implementation
func (s *service) GetServiceCodesForSchedule(scheduleID uint64, churchID uint64) ([]models.ServiceCode, error) {
    return s.repo.GetServiceCodesForSchedule(scheduleID, churchID)
}
```

**Step 3: Add handler method**

In `schedule_handler.go`:

```go
func (h *ScheduleHandler) GetServiceCodes(c *fiber.Ctx) error {
    id, err := c.ParamsInt("id")
    if err != nil {
        return respondError(c, fiber.StatusBadRequest, "ID inválido")
    }
    churchID := middleware.GetChurchID(c)
    codes, err := h.service.GetServiceCodesForSchedule(uint64(id), churchID)
    if err != nil {
        return handleError(c, err, "Erro interno do servidor")
    }
    return c.JSON(codes)
}
```

**Step 4: Register route**

In `routes.go`, near other schedule routes:

```go
protected.Get("/schedules/:id/service-codes", scheduleHandler.GetServiceCodes)
```

**Step 5: Verify**

```bash
cd backend && go build ./cmd/api/...
```

Expected: Build succeeds.

**Step 6: Commit**

```bash
git add backend/internal/
git commit -m "feat(api): add GET /schedules/:id/service-codes endpoint"
```

---

## Task 2: Mobile — New DTOs

**Goal:** Add DTOs for service codes, role counts, and positions endpoint.

**Files:**
- Modify: `app/src/main/java/br/com/leogsouza/escalav/data/remote/dto/RestrictionDto.kt`
- Modify: `app/src/main/java/br/com/leogsouza/escalav/data/remote/dto/ScheduleDto.kt`

**Step 1: Add ServiceCodeDto and RoleCountDto**

In `RestrictionDto.kt`, add after existing DTOs:

```kotlin
@JsonClass(generateAdapter = true)
data class ServiceCodeDto(
    val code: String,
    @Json(name = "name_pt") val namePt: String,
    @Json(name = "name_en") val nameEn: String,
    @Json(name = "day_of_week") val dayOfWeek: String,
    val period: String,
    @Json(name = "display_order") val displayOrder: Int
)

@JsonClass(generateAdapter = true)
data class RoleCountDto(
    @Json(name = "role_id") val roleId: Int,
    @Json(name = "role_name") val roleName: String,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class SpecificDateEntryDto(
    val date: String,
    @Json(name = "positionID") val positionId: Int?,
    val notes: String?
)

@JsonClass(generateAdapter = true)
data class DateRangeEntryDto(
    val start: String,
    val end: String,
    @Json(name = "positionID") val positionId: Int?
)
```

Note: `SpecificDateDto` and `DateRangeEntryDto` already exist in this file. Check if they need renaming or can be reused. The existing `SpecificDateDto` and `DateRangeEntryDto` should work as-is.

**Step 2: Verify build**

```bash
cd <project-root> && ./gradlew compileDebugKotlin
```

**Step 3: Commit**

```bash
git add app/src/main/java/
git commit -m "feat(dto): add ServiceCodeDto and RoleCountDto"
```

---

## Task 3: Mobile — ApiService New Endpoints

**Goal:** Add new API methods for service codes, role counts, positions by role, and update restrictions endpoint to support admin filtering.

**Files:**
- Modify: `app/src/main/java/br/com/leogsouza/escalav/data/remote/api/ApiService.kt`

**Step 1: Add new endpoints**

```kotlin
// Schedule service codes
@GET("schedules/{scheduleId}/service-codes")
suspend fun getScheduleServiceCodes(
    @Path("scheduleId") scheduleId: Int
): List<ServiceCodeDto>

// Restriction role counts
@GET("restrictions/role-counts")
suspend fun getRestrictionRoleCounts(
    @Query("schedule_id") scheduleId: Int
): List<RoleCountDto>

// Positions by role (for specific date position picker)
@GET("positions")
suspend fun getPositionsByRole(
    @Query("role_id") roleId: Int
): List<PositionDto>

// Get volunteer by ID (for loading positions based on roles)
@GET("volunteers/{id}")
suspend fun getVolunteerById(
    @Path("id") id: Int
): VolunteerDto
```

Also update existing `getRestrictions` to support `role_ids` and search:

```kotlin
@GET("restrictions")
suspend fun getRestrictions(
    @Query("page") page: Int? = null,
    @Query("page_size") pageSize: Int? = null,
    @Query("schedule_id") scheduleId: Int? = null,
    @Query("volunteer_id") volunteerId: Int? = null,
    @Query("role_ids") roleIds: String? = null,
    @Query("q") searchQuery: String? = null,
    @Query("active_only") activeOnly: Boolean? = null
): PaginatedRestrictionsDto
```

**Step 2: Verify build**

```bash
./gradlew compileDebugKotlin
```

**Step 3: Commit**

```bash
git add app/src/main/java/
git commit -m "feat(api): add service codes, role counts, and positions endpoints"
```

---

## Task 4: Mobile — RestrictionsListViewModel

**Goal:** New independent ViewModel for the list screen with schedule context, role filtering, and search.

**Files:**
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/RestrictionsListViewModel.kt`

**Step 1: Create the ViewModel**

```kotlin
package br.com.leogsouza.escalav.ui.restrictions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestrictionsListUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val restrictions: List<RestrictionDto> = emptyList(),
    val schedule: ScheduleDto? = null,
    val roleCounts: List<RoleCountDto> = emptyList(),
    val selectedRoleId: Int? = null,
    val searchQuery: String = "",
    val totalItems: Int = 0
)

@HiltViewModel
class RestrictionsListViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(RestrictionsListUiState())
    val state: StateFlow<RestrictionsListUiState> = _state

    private var searchJob: Job? = null

    fun loadInitialData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val schedules = api.getActiveSchedules()
                val schedule = schedules.firstOrNull()
                _state.value = _state.value.copy(schedule = schedule)

                if (schedule != null) {
                    val roleCounts = api.getRestrictionRoleCounts(schedule.id)
                    _state.value = _state.value.copy(roleCounts = roleCounts)
                }
                loadRestrictions()
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    private suspend fun loadRestrictions() {
        val s = _state.value
        val scheduleId = s.schedule?.id
        try {
            val result = api.getRestrictions(
                page = 1,
                pageSize = 50,
                scheduleId = scheduleId,
                roleIds = s.selectedRoleId?.toString(),
                searchQuery = s.searchQuery.ifBlank { null }
            )
            _state.value = _state.value.copy(
                loading = false,
                restrictions = result.data,
                totalItems = result.pagination.totalItems
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = e.message)
        }
    }

    fun filterByRole(roleId: Int?) {
        _state.value = _state.value.copy(
            selectedRoleId = if (_state.value.selectedRoleId == roleId) null else roleId,
            loading = true
        )
        viewModelScope.launch { loadRestrictions() }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _state.value = _state.value.copy(loading = true)
            loadRestrictions()
        }
    }

    fun deleteRestriction(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteRestriction(id)
                loadInitialData() // refresh list + counts
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}
```

**Step 2: Verify build**

```bash
./gradlew compileDebugKotlin
```

**Step 3: Commit**

```bash
git add app/src/main/java/
git commit -m "feat(vm): add RestrictionsListViewModel with role filtering and search"
```

---

## Task 5: Mobile — RestrictionsFormViewModel

**Goal:** New independent ViewModel for the form with schedule resolution, volunteer search, service codes, positions, and rules_json assembly.

**Files:**
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/RestrictionsFormViewModel.kt`

**Step 1: Create the ViewModel**

This is the most complex ViewModel. Key design decisions:
- Resolves schedule independently (no dependency on list VM)
- Decodes admin's church context from JWT for data scoping
- Handles volunteer search with debounce
- Loads service codes per schedule
- Loads positions per volunteer's roles
- Assembles rules_json from structured form state on save
- Appends `T00:00:00Z` to dates matching web behavior

The ViewModel should expose:
- `formState: StateFlow<RestrictionFormUiState>` with all form fields
- `volunteerResults: StateFlow<List<VolunteerDto>>` for search dropdown
- `loadFormData(restrictionId: Int?)` — loads schedule, types, codes, and optionally existing restriction
- `searchVolunteers(query: String)` — debounced search
- `selectVolunteer(volunteer: VolunteerDto)` — sets volunteer + loads positions
- `clearVolunteer()` — resets volunteer selection
- `saveRestriction(...)` — validates, assembles rules_json, creates/updates

State class:

```kotlin
data class RestrictionFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val schedule: ScheduleDto? = null,
    val restrictionTypes: List<RestrictionTypeDto> = emptyList(),
    val serviceCodes: List<ServiceCodeDto> = emptyList(),
    val positions: List<PositionDto> = emptyList(),
    val restriction: RestrictionDto? = null,
    val selectedVolunteer: VolunteerDto? = null,
    // Parsed rules (populated when editing existing restriction)
    val ruleMode: String = "exclude",
    val selectedServiceCodes: List<String> = emptyList(),
    val dayPattern: String = "",
    val specificDates: List<SpecificDateDto> = emptyList(),
    val dateRanges: List<DateRangeEntryDto> = emptyList()
)
```

For `saveRestriction`, the ViewModel receives the structured rule state from the composable and builds the JSON:

```kotlin
fun saveRestriction(
    volunteerId: Int,
    restrictionTypeId: Int,
    description: String,
    ruleMode: String,
    selectedServiceCodes: List<String>,
    dayPattern: String,
    specificDates: List<SpecificDateDto>,
    dateRanges: List<DateRangeEntryDto>,
    active: Boolean,
    fixed: Boolean,
    restrictionId: Int?
)
```

The rules JSON assembly logic:

```kotlin
private fun buildRulesJson(
    ruleMode: String,
    serviceCodes: List<String>,
    dayPattern: String,
    specificDates: List<SpecificDateDto>,
    dateRanges: List<DateRangeEntryDto>
): String {
    val conditionCount = listOf(
        serviceCodes.isNotEmpty(),
        dayPattern.isNotBlank(),
        specificDates.isNotEmpty(),
        dateRanges.isNotEmpty()
    ).count { it }

    val rules = mutableMapOf<String, Any>(
        "mode" to ruleMode,
        "operator" to if (ruleMode == "include" && conditionCount > 1) "AND" else "OR"
    )
    if (serviceCodes.isNotEmpty()) rules["serviceCodes"] = serviceCodes
    if (dayPattern.isNotBlank()) rules["dayPattern"] = dayPattern
    if (specificDates.isNotEmpty()) {
        rules["specificDates"] = specificDates.map { mapOf(
            "date" to (if ("T" in it.date) it.date else "${it.date}T00:00:00Z"),
            "positionID" to it.positionId,
            "notes" to (it.notes ?: "")
        )}
    }
    if (dateRanges.isNotEmpty()) {
        rules["dateRanges"] = dateRanges.map { mapOf(
            "start" to (if ("T" in it.start) it.start else "${it.start}T00:00:00Z"),
            "end" to (if ("T" in it.end) it.end else "${it.end}T00:00:00Z"),
            "positionID" to it.positionId
        )}
    }

    return JSONObject(rules as Map<*, *>).toString()
}
```

**Step 2: Implement the full ViewModel**

Reference the web's `RestrictionSlidePanel.tsx` lines 221-293 for save logic and `buildAndSave()` pattern.

**Step 3: Verify build**

```bash
./gradlew compileDebugKotlin
```

**Step 4: Commit**

```bash
git add app/src/main/java/
git commit -m "feat(vm): add RestrictionsFormViewModel with rules builder and volunteer search"
```

---

## Task 6: Mobile — Shared UI Components

**Goal:** Create reusable composables for the restriction screens.

**Files:**
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/components/ScheduleBanner.kt`
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/components/RulesSummary.kt`
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/components/RestrictionCard.kt`
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/components/VolunteerSearchField.kt`
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/components/ServiceCodeChips.kt`
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/components/DayPatternSelector.kt`
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/components/SpecificDateSection.kt`
- Create: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/components/DateRangeSection.kt`

### 6a: ScheduleBanner

Blue-tinted card showing schedule name, date range, and optional item count. Used by both list and form screens.

```kotlin
@Composable
fun ScheduleBanner(
    schedule: ScheduleDto?,
    itemCount: Int? = null, // shown on list, hidden on form
    modifier: Modifier = Modifier
)
```

Design: `Color(0xFFEFF6FF)` background, `Color(0xFF1E40AF)` text, calendar icon, formatted date range in pt-BR.

### 6b: RulesSummary

Parses `rulesJson: String?` and returns a human-readable composable with mode badge + conditions.

```kotlin
@Composable
fun RulesSummary(rulesJson: String?, modifier: Modifier = Modifier)

// Also expose utility for text-only (used in card)
fun parseRulesSummaryText(rulesJson: String?): Pair<String, String>
// Returns: ("Não pode" | "Pode", "DM, DN · 3 datas específicas")
```

Parsing logic:
- `mode == "exclude"` → "Não pode:"
- `mode == "include"` → "Pode:"
- Append service codes joined by ", "
- Append day pattern label
- Append "N datas específicas" if specific dates present
- Append "N períodos" if date ranges present
- If no rules → "Sem regras definidas"

### 6c: RestrictionCard

Rich card with volunteer avatar, name, type/status badges, rules summary, fixed indicator, and edit/delete actions.

```kotlin
@Composable
fun RestrictionCard(
    restriction: RestrictionDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
)
```

Uses `initials()` and `colorForVolunteer()` helpers from EventDetailScreen (extract to shared util or duplicate in-component). Avatar palette: same `AvatarPalette` from EventDetailScreen.

### 6d: VolunteerSearchField

Search field with debounced dropdown showing volunteer results.

```kotlin
@Composable
fun VolunteerSearchField(
    selectedVolunteer: VolunteerDto?,
    searchResults: List<VolunteerDto>,
    onSearchQueryChange: (String) -> Unit,
    onVolunteerSelected: (VolunteerDto) -> Unit,
    onClear: () -> Unit,
    isSearching: Boolean = false,
    modifier: Modifier = Modifier
)
```

Two states: empty (shows search input) and selected (shows chip with clear button).

### 6e: ServiceCodeChips

FilterChip FlowRow for service codes.

```kotlin
@Composable
fun ServiceCodeChips(
    availableCodes: List<ServiceCodeDto>,
    selectedCodes: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

### 6f: DayPatternSelector

Radio group for day patterns with tap-to-deselect behavior.

```kotlin
@Composable
fun DayPatternSelector(
    selectedPattern: String,
    onPatternChange: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

Options: even, odd, last_week, first_week (labels in Portuguese matching web).

### 6g: SpecificDateSection

List of date entries + [+] button that opens bottom sheet with DatePicker + position dropdown.

```kotlin
@Composable
fun SpecificDateSection(
    dates: List<SpecificDateDto>,
    positions: List<PositionDto>,
    scheduleStartDate: String?,
    scheduleEndDate: String?,
    onAdd: (SpecificDateDto) -> Unit,
    onRemove: (Int) -> Unit, // index
    modifier: Modifier = Modifier
)
```

Bottom sheet contains: Material 3 DatePickerDialog (constrained to schedule dates), position ExposedDropdownMenu (optional, includes "Todas as posições"), notes OutlinedTextField.

### 6h: DateRangeSection

Same pattern as SpecificDateSection but for date ranges (start + end).

```kotlin
@Composable
fun DateRangeSection(
    ranges: List<DateRangeEntryDto>,
    positions: List<PositionDto>,
    scheduleStartDate: String?,
    scheduleEndDate: String?,
    onAdd: (DateRangeEntryDto) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

**Step: Build and commit after all components**

```bash
./gradlew compileDebugKotlin
git add app/src/main/java/
git commit -m "feat(ui): add shared restriction composable components"
```

---

## Task 7: Mobile — Rewrite RestrictionsScreen (List)

**Goal:** Replace current list screen with rich cards, schedule banner, role filtering, and search.

**Files:**
- Rewrite: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/RestrictionsScreen.kt`

**Step 1: Rewrite the screen**

Key structural changes:
- Use `RestrictionsListViewModel` instead of old `RestrictionsViewModel`
- Add `ScheduleBanner` at top
- Add search `OutlinedTextField` below banner
- Add horizontal `LazyRow` of `FilterChip` for role counts (first chip is "Todos" with total count)
- Replace simple `RestrictionListItem` with `RestrictionCard`
- Keep existing delete confirmation dialog pattern
- Keep FAB for new restriction

```kotlin
@Composable
fun RestrictionsScreen(
    onNewRestriction: () -> Unit,
    onEditRestriction: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: RestrictionsListViewModel = hiltViewModel()
) { ... }
```

**Step 2: Verify build**

```bash
./gradlew compileDebugKotlin
```

**Step 3: Commit**

```bash
git add app/src/main/java/
git commit -m "feat(ui): rewrite RestrictionsScreen with rich cards and role filtering"
```

---

## Task 8: Mobile — Rewrite RestrictionFormScreen

**Goal:** Replace current basic form with full rules builder matching the web's functionality.

**Files:**
- Rewrite: `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/RestrictionFormScreen.kt`

**Step 1: Rewrite the screen**

Key structural changes:
- Use `RestrictionsFormViewModel` instead of old shared ViewModel
- Single scrollable `LazyColumn` (or `Column` with `verticalScroll`) with sections
- Schedule banner at top
- Volunteer search field (replaces JWT-derived ID)
- Type dropdown (keep existing pattern)
- Description multiline field
- Rules section with divider header:
  - SegmentedButton for mode (PODE SERVIR / NÃO PODE)
  - Helper text that changes based on mode
  - ServiceCodeChips
  - DayPatternSelector
  - SpecificDateSection
  - DateRangeSection
- Configuration section:
  - Active switch
  - Fixed switch ("Manter para próximas escalas")
- Save button that calls ViewModel's `saveRestriction()` with all structured state

Form state is held locally in the composable (like current pattern with `remember { mutableStateOf() }`), and passed to ViewModel on save.

When editing: `LaunchedEffect(formState.restriction)` populates local state from existing restriction's parsed `rulesJson`.

```kotlin
@Composable
fun RestrictionFormScreen(
    restrictionId: Int?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RestrictionsFormViewModel = hiltViewModel()
) { ... }
```

**Step 2: Verify build**

```bash
./gradlew compileDebugKotlin
```

**Step 3: Commit**

```bash
git add app/src/main/java/
git commit -m "feat(ui): rewrite RestrictionFormScreen with full rules builder"
```

---

## Task 9: Mobile — Update Navigation

**Goal:** Update AppNavigation to use new ViewModels and ensure proper screen routing.

**Files:**
- Modify: `app/src/main/java/br/com/leogsouza/escalav/ui/AppNavigation.kt`

**Step 1: Update imports and composable calls**

The navigation routes stay the same. The composables have the same signatures for `onNewRestriction`, `onEditRestriction`, `onBack`, `onSaved`. The ViewModels are injected via `hiltViewModel()` inside each composable, so navigation doesn't change structurally.

Verify that imports reference new screen composables correctly (they're in the same package, so imports shouldn't change).

**Step 2: Delete old ViewModel**

Delete `app/src/main/java/br/com/leogsouza/escalav/ui/restrictions/RestrictionsViewModel.kt` — replaced by `RestrictionsListViewModel` + `RestrictionsFormViewModel`.

**Step 3: Verify build**

```bash
./gradlew compileDebugKotlin
```

**Step 4: Commit**

```bash
git add app/src/main/java/
git commit -m "refactor(nav): clean up navigation and delete old RestrictionsViewModel"
```

---

## Task 10: Update AGENTS.md

**Goal:** Update AGENTS.md to reflect new architecture and document known patterns.

**Files:**
- Modify: `AGENTS.md`

**Step 1: Update sections**

Key updates:
- **Architecture section**: Document the two-ViewModel pattern for restrictions. Add `components/` subfolder under `restrictions/`.
- **End-to-End Data Flow**: Update restriction flow to describe admin volunteer search + rules builder + `rules_json` assembly.
- **Conventions**: Document `rules_json` structure and parsing pattern. Document color palette constants.
- **High-Impact Integration Notes**: Remove the warning about `RestrictionFormScreen` depending on `listState` (bug is fixed). Add note about `GET /schedules/:id/service-codes` backend endpoint. Document date timezone handling (`T00:00:00Z` suffix).
- **Safe Change Strategy**: Add guidance for adding new rule types to the builder.

**Step 2: Commit**

```bash
git add AGENTS.md
git commit -m "docs: update AGENTS.md for restriction feature redesign"
```

---

## Task Summary

| # | Task | Scope | Est. Complexity |
|---|---|---|---|
| 1 | Backend service codes endpoint | Backend (Go) | Low |
| 2 | New DTOs | Mobile (Kotlin) | Low |
| 3 | ApiService new endpoints | Mobile (Kotlin) | Low |
| 4 | RestrictionsListViewModel | Mobile (Kotlin) | Medium |
| 5 | RestrictionsFormViewModel | Mobile (Kotlin) | High |
| 6 | Shared UI components (8 composables) | Mobile (Compose) | High |
| 7 | Rewrite RestrictionsScreen | Mobile (Compose) | Medium |
| 8 | Rewrite RestrictionFormScreen | Mobile (Compose) | High |
| 9 | Navigation cleanup + delete old VM | Mobile (Kotlin) | Low |
| 10 | AGENTS.md update | Docs | Low |

**Critical path:** Tasks 1-3 are prerequisites. Task 4-5 depend on 2-3. Task 6-8 depend on 4-5. Task 9 depends on 7-8.

**Parallelizable:** Tasks 4+5 can be done in parallel. Tasks 6a-6h can be done in parallel.
