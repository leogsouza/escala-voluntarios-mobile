package br.com.leogsouza.escalav.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.EventDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: String,
    onEventClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val state by viewModel.dayState.collectAsState()

    LaunchedEffect(date) { viewModel.loadEventsForDay(date) }

    Scaffold(
        containerColor = Color(0xFFF3F4F6),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = formatDayTitle(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                state.events.isEmpty() -> Text(
                    "Nenhum evento neste dia.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.events) { event ->
                        StyledEventCard(event = event, onClick = { onEventClick(event.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StyledEventCard(event: EventDto, onClick: () -> Unit) {
    val gradientColors = headerGradientColors(event)
    val timeText = if (event.time.length >= 5) event.time.substring(0, 5) else event.time

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                // time badge
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = timeText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = event.service,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (event.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = event.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun formatDayTitle(dateStr: String): String = try {
    val date   = LocalDate.parse(dateStr.substring(0, 10))
    val locale = Locale("pt", "BR")
    val weekday = date.format(DateTimeFormatter.ofPattern("EEEE", locale))
        .replaceFirstChar { it.uppercase() }
    val month = date.format(DateTimeFormatter.ofPattern("MMMM", locale))
        .replaceFirstChar { it.uppercase() }
    "$weekday, ${date.dayOfMonth.toString().padStart(2, '0')} de $month"
} catch (_: Exception) {
    dateStr
}
