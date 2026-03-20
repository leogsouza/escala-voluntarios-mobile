package br.com.leogsouza.escalav.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.EventDto
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ─── colour tokens ────────────────────────────────────────────────────────────
private val GridBorderColor  = Color(0xFFE5E7EB)
private val TodayCircle      = Color(0xFF2563EB)
private val CurrentDayColor  = Color(0xFF111827)
private val OverflowDayColor = Color(0xFFD1D5DB)

// ─── screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(
    onDayClick: (String) -> Unit,
    onEventClick: (Int) -> Unit,
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
                        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = "Restrições")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── month navigation ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.changeMonth(state.selectedMonth.minusMonths(1)) },
                    enabled = state.canGoToPreviousMonth
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Mês anterior",
                        tint = if (state.canGoToPreviousMonth)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                Text(
                    text = state.selectedMonth.month
                        .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                        .replaceFirstChar { it.uppercase() } + " ${state.selectedMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { viewModel.changeMonth(state.selectedMonth.plusMonths(1)) },
                    enabled = state.canGoToNextMonth
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Próximo mês",
                        tint = if (state.canGoToNextMonth)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // ── day-of-week header row ────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB").forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            HorizontalDivider(color = GridBorderColor)

            // ── calendar body ─────────────────────────────────────────────────
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

// ─── grid ─────────────────────────────────────────────────────────────────────
@Composable
private fun CalendarGrid(
    month: LocalDate,
    eventsByDate: Map<String, List<EventDto>>,
    onDayClick: (String) -> Unit,
    onEventClick: (Int) -> Unit
) {
    val firstDay        = month.withDayOfMonth(1)
    val daysInMonth     = month.lengthOfMonth()
    val startOffset     = firstDay.dayOfWeek.value % 7   // Sun=0…Sat=6
    val totalUsed       = startOffset + daysInMonth
    val trailingCells   = if (totalUsed % 7 == 0) 0 else 7 - (totalUsed % 7)
    val totalCells      = totalUsed + trailingCells
    val totalRows       = totalCells / 7

    val prevMonth       = month.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()

    // Using Column+Row instead of LazyVerticalGrid so that
    // height(IntrinsicSize.Max) can equalise every cell in a row,
    // ensuring borders always align at the bottom.
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(totalRows) { rowIdx ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)   // all cells in this row share the same height
            ) {
                repeat(7) { colIdx ->
                    val index = rowIdx * 7 + colIdx
                    when {
                        index < startOffset -> OverflowDayCell(
                            day      = daysInPrevMonth - startOffset + index + 1,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        index >= startOffset + daysInMonth -> OverflowDayCell(
                            day      = index - startOffset - daysInMonth + 1,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        else -> {
                            val day     = index - startOffset + 1
                            val dateStr = "%d-%02d-%02d".format(month.year, month.monthValue, day)
                            val events  = eventsByDate[dateStr] ?: emptyList()
                            val isToday = dateStr == LocalDate.now().toString()
                            CurrentMonthDayCell(
                                day          = day,
                                dateStr      = dateStr,
                                events       = events,
                                isToday      = isToday,
                                onDayClick   = onDayClick,
                                onEventClick = onEventClick,
                                modifier     = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── individual cells ─────────────────────────────────────────────────────────
@Composable
private fun OverflowDayCell(day: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(BorderStroke(0.5.dp, GridBorderColor))
            .padding(4.dp)
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = OverflowDayColor
        )
    }
}

@Composable
private fun CurrentMonthDayCell(
    day: Int,
    dateStr: String,
    events: List<EventDto>,
    isToday: Boolean,
    onDayClick: (String) -> Unit,
    onEventClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasEvents = events.isNotEmpty()

    Column(
        modifier = modifier
            .border(BorderStroke(0.5.dp, GridBorderColor))
            .clickable(enabled = hasEvents) {
                if (events.size == 1) onEventClick(events.first().id)
                else onDayClick(dateStr)
            }
            .padding(4.dp)
    ) {
        // day number (blue filled circle for today)
        Box(
            modifier = if (isToday)
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(TodayCircle)
            else
                Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) Color.White else CurrentDayColor
            )
        }

        Spacer(Modifier.height(3.dp))

        // event pills (max 3 visible + overflow counter)
        val displayEvents = events.take(3)
        val overflow      = events.size - 3
        displayEvents.forEach { event ->
            EventPill(event = event, onClick = { onEventClick(event.id) })
            Spacer(Modifier.height(2.dp))
        }
        if (overflow > 0) {
            Text(
                text = "+$overflow",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
private fun EventPill(event: EventDto, onClick: () -> Unit) {
    val (bgColor, textColor) = eventPillColors(event)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.service,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

