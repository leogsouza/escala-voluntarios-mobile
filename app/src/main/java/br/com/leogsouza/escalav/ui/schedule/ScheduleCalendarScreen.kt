package br.com.leogsouza.escalav.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(
    onDayClick: (String) -> Unit,
    onRestrictionsClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.schedule?.name ?: "Escala") },
                actions = {
                    IconButton(onClick = onRestrictionsClick) {
                        Icon(Icons.Filled.EventNote, contentDescription = "Restrições")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sair")
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
                IconButton(onClick = { viewModel.changeMonth(state.selectedMonth.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês anterior")
                }
                Text(
                    text = "${state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }} ${state.selectedMonth.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { viewModel.changeMonth(state.selectedMonth.plusMonths(1)) }) {
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
                    onDayClick = onDayClick
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
    eventsByDate: Map<String, List<Any>>,
    onDayClick: (String) -> Unit
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
                val hasEvents = eventsByDate.containsKey(dateStr)
                val isToday = dateStr == LocalDate.now().toString()

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable(enabled = hasEvents) { onDayClick(dateStr) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (hasEvents) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                            )
                        }
                    }
                }
            }
        }
    }
}
