package br.com.leogsouza.escalav.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.AssignmentDto
import br.com.leogsouza.escalav.data.remote.dto.EventDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayDetailUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val events: List<EventDto> = emptyList()
)

data class EventDetailUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val event: EventDto? = null,
    val assignments: List<AssignmentDto> = emptyList()
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _dayState = MutableStateFlow(DayDetailUiState())
    val dayState: StateFlow<DayDetailUiState> = _dayState

    private val _eventState = MutableStateFlow(EventDetailUiState())
    val eventState: StateFlow<EventDetailUiState> = _eventState

    fun loadEventsForDay(date: String) {
        viewModelScope.launch {
            _dayState.value = DayDetailUiState(loading = true)
            try {
                val parts = date.split("-")
                val events = api.getEventsByMonth(parts[0].toInt(), parts[1].toInt())
                val filtered = events.filter { it.date.startsWith(date) }
                _dayState.value = DayDetailUiState(events = filtered)
            } catch (e: Exception) {
                _dayState.value = DayDetailUiState(error = e.message)
            }
        }
    }

    fun loadEventDetail(eventId: Int) {
        viewModelScope.launch {
            _eventState.value = EventDetailUiState(loading = true)
            try {
                val assignments = api.getAssignmentsByEvent(eventId)
                val event = assignments.firstOrNull()?.event
                _eventState.value = EventDetailUiState(event = event, assignments = assignments)
            } catch (e: Exception) {
                _eventState.value = EventDetailUiState(error = e.message)
            }
        }
    }
}
