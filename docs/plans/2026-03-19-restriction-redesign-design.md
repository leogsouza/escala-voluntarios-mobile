# Restriction Feature Redesign — Design Document

**Date**: 2026-03-19
**Status**: Approved
**Scope**: Mobile app restriction list + form rewrite with full rules builder, admin volunteer search, and new backend endpoint

---

## Context

The mobile restriction feature currently captures only description + type. The backend's `rules_json` field supports a rich constraint system (service codes, day patterns, specific dates, date ranges, positions) that the auto-scheduler algorithm depends on. Without structured rules, restrictions created on mobile have no effect on automated scheduling.

Additionally, the form has a critical ViewModel scoping bug where save silently fails because `scheduleId`/`volunteerId` are null in the form screen's ViewModel instance.

### Goals

1. Reach feature parity with the web's `RestrictionSlidePanel` for rule editing
2. Fix the ViewModel sharing bug (split into two independent ViewModels)
3. Add admin volunteer search (admin manages any volunteer's restrictions)
4. Add role-based filtering on the list screen
5. Create a new backend endpoint for schedule-scoped service codes
6. Design a polished mobile UI matching the EventDetailScreen's visual quality

### Non-Goals

- Weekday occurrence filters (FIRST, SECOND, etc.) — not in web form either
- Frequency limits — advanced feature, not in current web form
- Offline caching — separate concern
- Pagination — current 50-item fetch is sufficient for now

---

## Architecture

### ViewModel Split

**Current (broken):** Single `RestrictionsViewModel` shared across list and form via `hiltViewModel()`, but Navigation Compose creates separate instances per NavBackStackEntry. Form depends on list's `scheduleId`/`volunteerId` which are null.

**New:** Two independent ViewModels:

```
RestrictionsListViewModel
├── listState: StateFlow<RestrictionsListUiState>
│   ├── loading, error
│   ├── restrictions: List<RestrictionDto>
│   ├── schedule: ScheduleDto?
│   ├── roleCounts: List<RoleCountDto>
│   ├── selectedRoleId: Int?
│   └── searchQuery: String
├── loadRestrictions()
├── filterByRole(roleId: Int?)
├── search(query: String)
└── deleteRestriction(id: Int)

RestrictionsFormViewModel
├── formState: StateFlow<RestrictionFormUiState>
│   ├── loading, saving, error, saved
│   ├── schedule: ScheduleDto?
│   ├── restrictionTypes: List<RestrictionTypeDto>
│   ├── serviceCodes: List<ServiceCodeDto>
│   ├── positions: List<PositionDto>
│   ├── restriction: RestrictionDto? (edit mode)
│   └── volunteers: List<VolunteerDto> (search results)
├── loadFormData(restrictionId: Int?)  — resolves schedule independently
├── searchVolunteers(query: String)
├── loadPositionsForVolunteer(volunteerId: Int)
└── saveRestriction(...)  — assembles rules_json from structured state
```

Both resolve schedule via `GET /schedules/active` independently. No shared state dependency.

### New Backend Endpoint

**`GET /schedules/{scheduleId}/service-codes`**

Returns distinct `ServiceCode` objects that appear in the schedule's events, ordered by `display_order`.

```sql
SELECT DISTINCT sc.*
FROM events e
JOIN service_codes sc ON sc.code = e.service_code
WHERE e.schedule_id = ? AND e.deleted_at IS NULL
ORDER BY sc.display_order
```

Response: `List<ServiceCode>` — same shape as `GET /service-codes` but filtered to schedule.

Route: `protected.Get("/schedules/:id/service-codes", scheduleHandler.GetServiceCodes)`

### New/Modified Files

```
Backend:
  internal/handlers/schedule_handler.go    — ADD GetServiceCodes method
  internal/services/schedule/...           — ADD GetServiceCodesForSchedule
  internal/repositories/schedule_repo...   — ADD query
  internal/handlers/routes.go              — ADD route

Mobile:
  data/remote/api/ApiService.kt            — ADD endpoints
  data/remote/dto/RestrictionDto.kt        — ADD ServiceCodeDto, RoleCountDto
  data/remote/dto/ScheduleDto.kt           — ADD PositionDto updates if needed

  ui/restrictions/
  ├── RestrictionsScreen.kt                — REWRITE (rich cards, filters, search)
  ├── RestrictionsListViewModel.kt         — NEW
  ├── RestrictionFormScreen.kt             — REWRITE (full rules builder)
  ├── RestrictionFormViewModel.kt          — NEW
  ├── RestrictionsViewModel.kt             — DELETE (replaced by above two)
  └── components/
      ├── ScheduleBanner.kt                — NEW (shared schedule context)
      ├── RulesSummary.kt                  — NEW (rules_json → display text)
      ├── RestrictionCard.kt               — NEW (rich list item)
      ├── ServiceCodeChips.kt              — NEW (service code FilterChips)
      ├── DayPatternSelector.kt            — NEW (radio group)
      ├── SpecificDateSection.kt           — NEW (date list + add bottom sheet)
      ├── DateRangeSection.kt              — NEW (range list + add bottom sheet)
      └── VolunteerSearchField.kt          — NEW (search with dropdown)
```

---

## UI Design

### Color Palette (matching EventDetailScreen language)

| Element | Color | Token |
|---|---|---|
| Schedule banner bg | `Color(0xFFEFF6FF)` | Blue-50 |
| Schedule banner text | `Color(0xFF1E40AF)` | Blue-800 |
| Type badge bg | `Color(0xFFF3F4F6)` | Gray-100 |
| Active badge | `Color(0xFF10B981)` | Green-500 |
| Inactive badge | `Color(0xFF9CA3AF)` | Gray-400 |
| Fixed pin icon | `Color(0xFFF59E0B)` | Amber-500 |
| "Não pode" accent | `Color(0xFFEF4444)` | Red-500 |
| "Pode servir" accent | `Color(0xFF3B82F6)` | Blue-500 |
| Section dividers | `Color(0xFFE5E7EB)` | Gray-200 |
| Card border | `Color(0xFFE5E7EB)` | Same PanelBorder |

### Restriction List Screen

```
┌──────────────────────────────────┐
│ ◄  Restrições                    │  TopAppBar
├──────────────────────────────────┤
│ ┌── Schedule Banner ───────────┐ │
│ │ 📅 Escala Jan-Mar 2026       │ │
│ │    01/01/2026 - 31/03/2026   │ │
│ │    18 restrições cadastradas  │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌──────────────────────────────┐ │
│ │ 🔍 Buscar voluntário...      │ │  Search bar
│ └──────────────────────────────┘ │
│                                  │
│ [Todos 18] [Porteiro 5]         │  Role filter chips
│ [Organista 3] [Som 4] ►         │  (horizontal scroll)
│                                  │
│ ┌──────────────────────────────┐ │
│ │ JS  João da Silva            │ │  Avatar + name
│ │     ┌────────┐ ┌───────┐    │ │
│ │     │Trabalho│ │● Ativa│    │ │  Type + status pills
│ │     └────────┘ └───────┘    │ │
│ │     Não pode: DM, DN        │ │  Rules summary
│ │     3 datas específicas      │ │
│ │     📌 Fixa p/ próx. escala │ │  Fixed badge
│ │                     ✏️  🗑️  │ │
│ └──────────────────────────────┘ │
│                                  │
│                            [+]   │  FAB
└──────────────────────────────────┘
```

**Data flow:**
1. `GET /schedules/active` → resolve schedule
2. `GET /restrictions?schedule_id=X&page=1&page_size=50` → all restrictions (no volunteer filter — admin sees all)
3. `GET /restrictions/role-counts?schedule_id=X` → role badge counts
4. Role chip tap → re-fetch with `role_ids`
5. Search → debounced re-fetch with `q`

### Restriction Form Screen

Single scrollable screen with visual sections:

```
┌──────────────────────────────────┐
│ ◄  Nova Restrição                │
├──────────────────────────────────┤
│ ┌── Schedule Banner ───────────┐ │
│ │ 📅 Escala Jan-Mar 2026       │ │
│ │    01/01/2026 - 31/03/2026   │ │
│ └──────────────────────────────┘ │
│                                  │
│ ─── Informações Básicas ──────── │
│                                  │
│ Voluntário *                     │
│ ┌──────────────────────────────┐ │
│ │ 🔍 Digite para buscar...     │ │  Debounced search dropdown
│ └──────────────────────────────┘ │
│   After selection:               │
│ ┌──────────────────────────┬───┐ │
│ │ JS  João da Silva        │ ✕ │ │  Selected state with clear
│ └──────────────────────────┴───┘ │
│                                  │
│ Tipo de Restrição *              │
│ ┌──────────────────────────────┐ │
│ │ Selecione o tipo           ▾ │ │  ExposedDropdownMenu
│ └──────────────────────────────┘ │
│                                  │
│ Descrição *                      │
│ ┌──────────────────────────────┐ │
│ │                              │ │  OutlinedTextField multiline
│ └──────────────────────────────┘ │
│                                  │
│ ─── Regras de Disponibilidade ── │
│                                  │
│ ┌─────────────────┬────────────┐ │
│ │ ○ PODE SERVIR   │◉ NÃO PODE │ │  SegmentedButton
│ └─────────────────┴────────────┘ │
│ ⓘ Dias que NÃO pode servir      │
│                                  │
│ Códigos de Serviço               │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐    │  FilterChips (from schedule)
│ │ DM │ │ DN │ │ 3T │ │ 3N │    │
│ └────┘ └────┘ └────┘ └────┘    │
│ ┌────┐ ┌────┐ ┌────┐           │
│ │ 4N │ │ 6N │ │ SN │           │
│ └────┘ └────┘ └────┘           │
│                                  │
│ Padrão de Dias do Mês            │
│ ○ Apenas dias pares (2,4,6...)  │  RadioButtons
│ ○ Apenas dias ímpares (1,3,5...)│
│ ○ Apenas última semana          │
│ ○ Apenas primeira semana        │
│                                  │
│ ─── Datas Específicas ───── [+]  │
│ ┌──────────────────────────────┐ │
│ │ 📅 05/03/2026               │ │  Each entry: date + position
│ │    Posição: Porteiro      ✕  │ │  + remove button
│ ├──────────────────────────────┤ │
│ │ 📅 08/03/2026               │ │
│ │    Posição: Todas         ✕  │ │
│ └──────────────────────────────┘ │
│                                  │
│ ─── Períodos de Data ─────── [+] │
│ ┌──────────────────────────────┐ │
│ │ 📅 01/03 → 15/03/2026       │ │  Range: start → end
│ │    Posição: Todas         ✕  │ │  + position + remove
│ └──────────────────────────────┘ │
│                                  │
│ ─── Configurações ───────────── │
│                                  │
│ Restrição ativa           [━●]  │  Switch
│ Manter p/ próximas escalas[━●]  │  Switch (fixed field)
│                                  │
│ ┌──────────────────────────────┐ │
│ │          Salvar               │ │  Primary button
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

### Bottom Sheets

**Add Specific Date** — tapping [+]:
- Material 3 DatePickerDialog (constrained to schedule date range)
- Position dropdown (optional, loaded from volunteer's roles)
- Notes field (optional)
- "Adicionar" button

**Add Date Range** — tapping [+]:
- Start date field → DatePickerDialog
- End date field → DatePickerDialog (must be after start)
- Position dropdown (optional)
- "Adicionar" button

### Rules JSON Assembly

On save, structured state → JSON:

```kotlin
val rules = buildMap {
    put("mode", ruleMode)  // "exclude" or "include"
    val conditionCount = listOf(
        serviceCodes.isNotEmpty(),
        dayPattern.isNotBlank(),
        specificDates.isNotEmpty(),
        dateRanges.isNotEmpty()
    ).count { it }
    put("operator", if (ruleMode == "include" && conditionCount > 1) "AND" else "OR")
    if (serviceCodes.isNotEmpty()) put("serviceCodes", serviceCodes)
    if (dayPattern.isNotBlank()) put("dayPattern", dayPattern)
    if (specificDates.isNotEmpty()) put("specificDates", specificDatesWithTimezone)
    if (dateRanges.isNotEmpty()) put("dateRanges", dateRangesWithTimezone)
}
```

Dates appended with `T00:00:00Z` before saving (matching web behavior).

### Rules Summary Parser (for list cards)

```
{"mode":"exclude","serviceCodes":["DM","DN"],"specificDates":[...3]}
→ "Não pode: DM, DN · 3 datas específicas"

{"mode":"include","serviceCodes":["3N"],"dayPattern":"even"}
→ "Pode: 3N · Dias pares"

{"mode":"exclude","dateRanges":[{"start":"...","end":"..."}]}
→ "Não pode: 01/03 - 15/03"

null or empty
→ "Sem regras definidas"
```

---

## Form Validation

| Field | Rule | Error Message |
|---|---|---|
| Volunteer | Required, must be selected from search | "Selecione um voluntário" |
| Restriction type | Required, must select from dropdown | "Selecione o tipo de restrição" |
| Description | Required, non-blank | "Informe uma descrição" |
| Specific date | Must be within schedule date range | "Data fora do período da escala" |
| Date range end | Must be after start | "Data fim deve ser posterior à data início" |
| Rules section | Soft warning if completely empty | "Nenhuma regra definida (a restrição será apenas informativa)" |

---

## API Changes Summary

### New Backend Endpoint
- `GET /schedules/:id/service-codes` — distinct service codes for schedule's events

### New Mobile ApiService Methods
- `getScheduleServiceCodes(scheduleId)` → `List<ServiceCodeDto>`
- `getRestrictionRoleCounts(scheduleId)` → `List<RoleCountDto>`
- `getPositionsByRole(roleId)` → `List<PositionDto>` (if not already available)

### New DTOs
- `ServiceCodeDto` (code, namePt, dayOfWeek, period, displayOrder)
- `RoleCountDto` (roleId, roleName, count)

---

## Testing Considerations

No test framework is currently configured. At minimum, the following should be manually verified:

1. List loads with schedule context, role counts, and restriction cards
2. Role filter chips filter correctly
3. Search filters by volunteer name
4. Delete shows confirmation and removes from list
5. Form create flow: search volunteer → fill fields → add rules → save
6. Form edit flow: loads existing data including parsed rules
7. Service codes load correctly per schedule
8. Specific dates respect schedule date range boundaries
9. Date ranges validate end > start
10. Rules JSON assembles correctly matching web format
11. Fixed toggle persists correctly
12. Active toggle persists correctly
