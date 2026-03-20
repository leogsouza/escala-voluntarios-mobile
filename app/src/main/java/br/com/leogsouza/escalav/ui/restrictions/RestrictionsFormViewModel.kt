package br.com.leogsouza.escalav.ui.restrictions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.DateRangeEntryDto
import br.com.leogsouza.escalav.data.remote.dto.PositionDto
import br.com.leogsouza.escalav.data.remote.dto.RestrictionDto
import br.com.leogsouza.escalav.data.remote.dto.RestrictionRulesDto
import br.com.leogsouza.escalav.data.remote.dto.RestrictionTypeDto
import br.com.leogsouza.escalav.data.remote.dto.ScheduleDto
import br.com.leogsouza.escalav.data.remote.dto.ServiceCodeDto
import br.com.leogsouza.escalav.data.remote.dto.SpecificDateDto
import br.com.leogsouza.escalav.data.remote.dto.VolunteerDto
import br.com.leogsouza.escalav.data.remote.dto.firstDraftSchedule
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

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

@HiltViewModel
class RestrictionsFormViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _formState = MutableStateFlow(RestrictionFormUiState())
    val formState: StateFlow<RestrictionFormUiState> = _formState

    private val _volunteerResults = MutableStateFlow<List<VolunteerDto>>(emptyList())
    val volunteerResults: StateFlow<List<VolunteerDto>> = _volunteerResults

    private val _volunteerSearching = MutableStateFlow(false)
    val volunteerSearching: StateFlow<Boolean> = _volunteerSearching

    private var volunteerSearchJob: Job? = null

    fun loadFormData(restrictionId: Int?) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(loading = true, error = null)
            try {
                // Load schedule, types in parallel via sequential-but-fast calls
                val schedules = api.getSchedules(page = 1, pageSize = 50)
                val schedule = schedules.data.firstDraftSchedule()

                val types = api.getRestrictionTypes()

                val serviceCodes = if (schedule != null) {
                    api.getScheduleServiceCodes(schedule.id)
                } else {
                    emptyList()
                }

                // Optionally load existing restriction for edit mode
                val restriction = restrictionId?.let { api.getRestrictionById(it) }

                // Load volunteer details if editing
                val selectedVolunteer = restriction?.let {
                    try { api.getVolunteerById(it.volunteerId) } catch (e: Exception) { null }
                }

                // Parse existing rules if editing
                val parsedRules = restriction?.rulesJson?.let { parseRulesJson(it) }

                // Load positions based on volunteer's roles
                val positions = selectedVolunteer?.let { loadPositionsForVolunteer(it) } ?: emptyList()

                _formState.value = _formState.value.copy(
                    loading = false,
                    schedule = schedule,
                    restrictionTypes = types,
                    serviceCodes = serviceCodes,
                    positions = positions,
                    restriction = restriction,
                    selectedVolunteer = selectedVolunteer,
                    ruleMode = parsedRules?.mode ?: "exclude",
                    selectedServiceCodes = parsedRules?.serviceCodes ?: emptyList(),
                    dayPattern = parsedRules?.dayPattern ?: "",
                    specificDates = parsedRules?.specificDates ?: emptyList(),
                    dateRanges = parsedRules?.dateRanges ?: emptyList()
                )
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun searchVolunteers(query: String) {
        if (query.length < 2) {
            _volunteerResults.value = emptyList()
            return
        }
        volunteerSearchJob?.cancel()
        volunteerSearchJob = viewModelScope.launch {
            delay(300) // debounce
            _volunteerSearching.value = true
            try {
                _volunteerResults.value = api.searchVolunteers(query)
            } catch (e: Exception) {
                _volunteerResults.value = emptyList()
            } finally {
                _volunteerSearching.value = false
            }
        }
    }

    fun selectVolunteer(volunteer: VolunteerDto) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(selectedVolunteer = volunteer)
            _volunteerResults.value = emptyList()
            val positions = loadPositionsForVolunteer(volunteer)
            _formState.value = _formState.value.copy(positions = positions)
        }
    }

    fun clearVolunteer() {
        _formState.value = _formState.value.copy(selectedVolunteer = null, positions = emptyList())
        _volunteerResults.value = emptyList()
    }

    private suspend fun loadPositionsForVolunteer(volunteer: VolunteerDto): List<PositionDto> {
        return try {
            val roleId = volunteer.mainRoleId ?: return emptyList()
            api.getPositionsByRole(roleId)
        } catch (e: Exception) {
            emptyList()
        }
    }

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
    ) {
        val scheduleId = _formState.value.schedule?.id ?: run {
            _formState.value = _formState.value.copy(error = "Escala não encontrada")
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(saving = true, error = null)
            try {
                val rulesJson = buildRulesJson(
                    ruleMode = ruleMode,
                    serviceCodes = selectedServiceCodes,
                    dayPattern = dayPattern,
                    specificDates = specificDates,
                    dateRanges = dateRanges
                )

                val dto = RestrictionDto(
                    id = restrictionId,
                    volunteerId = volunteerId,
                    scheduleId = scheduleId,
                    description = description.trim(),
                    restrictionTypeId = restrictionTypeId,
                    restrictionType = null,
                    rulesJson = rulesJson,
                    active = active,
                    fixed = fixed,
                    volunteer = null
                )

                if (restrictionId == null) {
                    api.createRestriction(dto)
                } else {
                    api.updateRestriction(restrictionId, dto)
                }

                _formState.value = _formState.value.copy(saving = false, saved = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(saving = false, error = e.message)
            }
        }
    }

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

        val obj = JSONObject()
        obj.put("mode", ruleMode)
        obj.put("operator", if (ruleMode == "include" && conditionCount > 1) "AND" else "OR")

        if (serviceCodes.isNotEmpty()) {
            val arr = JSONArray()
            serviceCodes.forEach { arr.put(it) }
            obj.put("serviceCodes", arr)
        }

        if (dayPattern.isNotBlank()) {
            obj.put("dayPattern", dayPattern)
        }

        if (specificDates.isNotEmpty()) {
            val arr = JSONArray()
            specificDates.forEach { entry ->
                val item = JSONObject()
                item.put("date", if ("T" in entry.date) entry.date else "${entry.date}T00:00:00Z")
                item.put("positionID", entry.positionId ?: JSONObject.NULL)
                item.put("notes", entry.notes ?: "")
                arr.put(item)
            }
            obj.put("specificDates", arr)
        }

        if (dateRanges.isNotEmpty()) {
            val arr = JSONArray()
            dateRanges.forEach { range ->
                val item = JSONObject()
                item.put("start", if ("T" in range.start) range.start else "${range.start}T00:00:00Z")
                item.put("end", if ("T" in range.end) range.end else "${range.end}T00:00:00Z")
                item.put("positionID", range.positionId ?: JSONObject.NULL)
                arr.put(item)
            }
            obj.put("dateRanges", arr)
        }

        return obj.toString()
    }

    private fun parseRulesJson(json: String): RestrictionRulesDto? {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(RestrictionRulesDto::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
