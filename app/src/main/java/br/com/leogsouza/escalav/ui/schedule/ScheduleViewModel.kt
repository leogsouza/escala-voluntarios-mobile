package br.com.leogsouza.escalav.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.EventDto
import br.com.leogsouza.escalav.data.remote.dto.ScheduleDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CalendarUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val schedule: ScheduleDto? = null,
    val eventsByDate: Map<String, List<EventDto>> = emptyMap(),
    val selectedMonth: LocalDate = LocalDate.now().withDayOfMonth(1)
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state

    init { loadScheduleAndEvents() }

    fun changeMonth(month: LocalDate) {
        _state.value = _state.value.copy(selectedMonth = month)
        loadEventsForMonth(month)
    }

    private fun loadScheduleAndEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val schedules = api.getActiveSchedules()
                val schedule = schedules.firstOrNull()
                _state.value = _state.value.copy(schedule = schedule)
                loadEventsForMonth(_state.value.selectedMonth)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    private fun loadEventsForMonth(month: LocalDate) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val scheduleId = _state.value.schedule?.id
                val events = api.getEventsByMonth(month.year, month.monthValue, scheduleId)
                val byDate = events.groupBy { it.date.substring(0, 10) }
                _state.value = _state.value.copy(loading = false, eventsByDate = byDate)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}
