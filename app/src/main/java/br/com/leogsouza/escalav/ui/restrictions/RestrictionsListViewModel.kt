package br.com.leogsouza.escalav.ui.restrictions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.RestrictionDto
import br.com.leogsouza.escalav.data.remote.dto.RoleCountDto
import br.com.leogsouza.escalav.data.remote.dto.ScheduleDto
import br.com.leogsouza.escalav.data.remote.dto.firstDraftSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestrictionsListUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
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
                val schedules = api.getSchedules(page = 1, pageSize = 50)
                val schedule = schedules.data.firstDraftSchedule()
                _state.value = _state.value.copy(schedule = schedule)

                if (schedule != null) {
                    val roleCounts = api.getRestrictionRoleCounts(schedule.id)
                    _state.value = _state.value.copy(roleCounts = roleCounts)
                }
                loadRestrictions()
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, refreshing = false, error = e.message)
            }
        }
    }

    /** Pull-to-refresh: keeps the current list visible while fetching fresh data. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(refreshing = true, error = null)
            try {
                val schedules = api.getSchedules(page = 1, pageSize = 50)
                val schedule = schedules.data.firstDraftSchedule()
                _state.value = _state.value.copy(schedule = schedule)

                if (schedule != null) {
                    val roleCounts = api.getRestrictionRoleCounts(schedule.id)
                    _state.value = _state.value.copy(roleCounts = roleCounts)
                }
                loadRestrictions()
            } catch (e: Exception) {
                _state.value = _state.value.copy(refreshing = false, error = e.message)
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
                refreshing = false,
                restrictions = result.data,
                totalItems = result.pagination.totalItems
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, refreshing = false, error = e.message)
        }
    }

    fun filterByRole(roleId: Int?) {
        val newRoleId = if (_state.value.selectedRoleId == roleId) null else roleId
        _state.value = _state.value.copy(selectedRoleId = newRoleId, loading = true)
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
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}
