package br.com.leogsouza.escalav.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.EventDto
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(
    onDayClick: (String) -> Unit,
    onEventClick: (Int) -> Unit,
    onLogout: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.schedule?.name ?: "Escala") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.changeMonth(state.selectedMonth.minusMonths(1)) },
                    enabled = state.canGoToPreviousMonth
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês anterior")
                }
                Text(
                    text = "${state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }} ${state.selectedMonth.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = { viewModel.changeMonth(state.selectedMonth.plusMonths(1)) },
                    enabled = state.canGoToNextMonth
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo mês")
                }
            }

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                CalendarGrid(
                    month = state.selectedMonth,
                    eventsByDate = state.eventsByDate,
                    onDayClick = onDayClick,
                    onEventClick = onEventClick
                )
            }

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: LocalDate,
    eventsByDate: Map<String, List<EventDto>>,
    onDayClick: (String) -> Unit,
    onEventClick: (Int) -> Unit
) {
    val firstDay = month.withDayOfMonth(1)
    val daysInMonth = month.lengthOfMonth()
    // Sunday=0, shift from Java's Monday=1 convention
    val startOffset = (firstDay.dayOfWeek.value % 7)
    val totalCells = startOffset + daysInMonth

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        items(totalCells) { index ->
            if (index < startOffset) {
                Box(modifier = Modifier.aspectRatio(1f))
            } else {
                val day = index - startOffset + 1
                val dateStr = "${month.year}-${month.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                val events = eventsByDate[dateStr].orEmpty()
                val hasEvents = events.isNotEmpty()
                val isToday = dateStr == LocalDate.now().toString()

                Column(
                    modifier = Modifier
                        .aspectRatio(0.84f)
                        .padding(2.dp)
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clickable(enabled = hasEvents) { onDayClick(dateStr) },
                        textAlign = TextAlign.Center
                    )

                    events.take(2).forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                .height(16.dp)
                                .background(colorForEvent(event), shape = MaterialTheme.shapes.extraSmall)
                                .clickable { onEventClick(event.id) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = event.service,
                                maxLines = 1,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Text(
                                text = formatCalendarTime(event.time),
                                maxLines = 1,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    if (events.size > 2) {
                        Text(
                            text = "+${events.size - 2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .clickable { onDayClick(dateStr) },
                            textAlign = TextAlign.End
                        )
                    }

                    if (!hasEvents) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .align(Alignment.CenterHorizontally)
                                .background(MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}

private fun formatCalendarTime(raw: String): String {
    return if (raw.length >= 5) raw.substring(0, 5) else raw
}

private fun colorForEvent(event: EventDto): Color {
    val service = event.service.lowercase(Locale.getDefault())
    return when {
        service.contains("rjm") -> Color(0xFF8E44AD)
        service.contains("ensaio local") -> Color(0xFFE67E22)
        service.contains("culto") -> Color(0xFF2E86DE)
        else -> Color(0xFF607D8B)
    }
}

