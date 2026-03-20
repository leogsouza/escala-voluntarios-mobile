package br.com.leogsouza.escalav.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.EventDto
import br.com.leogsouza.escalav.data.remote.dto.ScheduleDto
import br.com.leogsouza.escalav.data.remote.dto.firstPublishedSchedule
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
    val selectedMonth: LocalDate = LocalDate.now().withDayOfMonth(1),
    val canGoToPreviousMonth: Boolean = false,
    val canGoToNextMonth: Boolean = false
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state

    init { loadScheduleAndEvents() }

    fun changeMonth(month: LocalDate) {
        if (!canNavigateToMonth(month)) return
        _state.value = _state.value.copy(selectedMonth = month)
        updateMonthNavigationAvailability(month)
        loadEventsForMonth(month)
    }

    private fun loadScheduleAndEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val schedules = api.getSchedules(page = 1, pageSize = 50)
                val schedule = schedules.data.firstPublishedSchedule()
                val initialMonth = resolveInitialMonth(schedule)
                _state.value = _state.value.copy(schedule = schedule, selectedMonth = initialMonth)
                updateMonthNavigationAvailability(initialMonth)
                loadEventsForMonth(initialMonth)
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

    private fun resolveInitialMonth(schedule: ScheduleDto?): LocalDate {
        if (schedule == null) return _state.value.selectedMonth
        val startDate = parseDate(schedule.startDate) ?: return _state.value.selectedMonth
        val endDate = parseDate(schedule.endDate) ?: return _state.value.selectedMonth
        val currentMonth = LocalDate.now().withDayOfMonth(1)
        return if (isMonthInsideRange(currentMonth, startDate, endDate)) {
            currentMonth
        } else {
            startDate.withDayOfMonth(1)
        }
    }

    private fun updateMonthNavigationAvailability(referenceMonth: LocalDate) {
        _state.value = _state.value.copy(
            canGoToPreviousMonth = canNavigateToMonth(referenceMonth.minusMonths(1)),
            canGoToNextMonth = canNavigateToMonth(referenceMonth.plusMonths(1))
        )
    }

    private fun canNavigateToMonth(month: LocalDate): Boolean {
        val schedule = _state.value.schedule ?: return false
        val startDate = parseDate(schedule.startDate) ?: return false
        val endDate = parseDate(schedule.endDate) ?: return false
        return isMonthInsideRange(month.withDayOfMonth(1), startDate, endDate)
    }

    private fun isMonthInsideRange(month: LocalDate, startDate: LocalDate, endDate: LocalDate): Boolean {
        val firstDayOfMonth = month.withDayOfMonth(1)
        val lastDayOfMonth = month.withDayOfMonth(month.lengthOfMonth())
        return !lastDayOfMonth.isBefore(startDate) && !firstDayOfMonth.isAfter(endDate)
    }

    private fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value.substring(0, 10))
    } catch (_: Exception) {
        null
    }
}
