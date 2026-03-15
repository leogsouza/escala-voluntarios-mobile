package br.com.leogsouza.escalav.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.AssignmentDto
import br.com.leogsouza.escalav.data.remote.dto.EventDto
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class RoleGroupUi(
    val roleName: String,
    val assignments: List<AssignmentDto>
)

private val ScreenBg = Color(0xFFF3F4F6)
private val PanelBorder = Color(0xFFE5E7EB)
private val RoleAccentBlue = Color(0xFF2563EB)

private val RoleDisplayOrder = listOf(
    "Porteiro",
    "Auxiliar da Porta",
    "Brigada Irmãos",
    "Brigada Irmãs",
    "Operador(a) de Som",
    "Zeladoria",
    "Instrutores de Música",
    "Organista"
)

private val AvatarPalette = listOf(
    Color(0xFF3B82F6),
    Color(0xFFA855F7),
    Color(0xFFEC4899),
    Color(0xFFFB7185),
    Color(0xFFF97316),
    Color(0xFF06B6D4)
)

private val RoleOrderIndex = RoleDisplayOrder
    .withIndex()
    .associate { normalizeRoleName(it.value) to it.index }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    onBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val state by viewModel.eventState.collectAsState()

    LaunchedEffect(eventId) { viewModel.loadEventDetail(eventId) }

    Scaffold(containerColor = ScreenBg) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                else -> {
                    val groupedAssignments = state.assignments
                        .groupBy {
                            val rawRole = it.position?.role?.name ?: "Sem função"
                            canonicalRoleName(rawRole)
                        }
                        .map {
                            RoleGroupUi(
                                roleName = it.key,
                                assignments = it.value.sortedBy { assignment -> assignment.position?.id ?: Int.MAX_VALUE }
                            )
                        }
                        .sortedWith(
                            compareBy<RoleGroupUi> { RoleOrderIndex[normalizeRoleName(it.roleName)] ?: Int.MAX_VALUE }
                                .thenBy { it.roleName }
                        )

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { EventHeader(state.event, onBack = onBack) }

                        if (groupedAssignments.isEmpty()) {
                            item {
                                Text(
                                    text = "Nenhum voluntário escalado.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            items(groupedAssignments) { group ->
                                RoleSection(group)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventHeader(event: EventDto?, onBack: () -> Unit) {
    val dateText = event?.date?.let(::formatEventDate) ?: "Data não disponível"
    val timeText = event?.time?.let(::formatEventTime) ?: "--:--"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF8A2BE2), Color(0xFF6A0DAD))
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = timeText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.large,
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fechar",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = event?.service ?: "Evento",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun RoleSection(group: RoleGroupUi) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(PanelBorder)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoleAccentBlue)
                    )
                    Text(
                        text = group.roleName.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = volunteerCounterLabel(group.assignments.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            group.assignments.forEachIndexed { index, assignment ->
                VolunteerRow(assignment)
                if (index < group.assignments.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun VolunteerRow(assignment: AssignmentDto) {
    val displayName = assignment.volunteer?.name?.ifBlank { null }
        ?: assignment.volunteer?.fullName
        ?: "Sem nome"
    val avatarColor = colorForVolunteer(displayName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(PanelBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials(displayName),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = (assignment.position?.name ?: "Sem posição").uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoleAccentBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isConfirmed(assignment.status)) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Confirmado",
                    tint = Color(0xFF10B981)
                )
            }
        }
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "--"
        parts.size == 1 -> parts.first().take(2).uppercase(Locale.getDefault())
        else -> "${parts.first().first()}${parts.last().first()}".uppercase(Locale.getDefault())
    }
}

private fun isConfirmed(status: String): Boolean {
    val normalized = status.uppercase(Locale.getDefault())
    return normalized == "CONFIRMED" || normalized == "APPROVED"
}

private fun formatEventDate(raw: String): String {
    return try {
        val date = LocalDate.parse(raw.substring(0, 10))
        val locale = Locale("pt", "BR")
        val weekday = date.format(DateTimeFormatter.ofPattern("EEEE", locale)).replaceFirstChar { it.uppercase() }
        val month = date.format(DateTimeFormatter.ofPattern("MMMM", locale)).replaceFirstChar { it.uppercase() }
        "$weekday, ${date.dayOfMonth.toString().padStart(2, '0')} de $month, ${date.year}"
    } catch (_: Exception) {
        raw.take(10)
    }
}

private fun formatEventTime(raw: String): String {
    return if (raw.length >= 5) raw.substring(0, 5) else raw
}

private fun volunteerCounterLabel(count: Int): String {
    return if (count == 1) "1 Voluntário" else "$count Voluntários"
}

private fun canonicalRoleName(raw: String): String {
    val normalized = normalizeRoleName(raw)
    return RoleDisplayOrder.firstOrNull { normalizeRoleName(it) == normalized } ?: raw
}

private fun normalizeRoleName(value: String): String {
    val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
    return withoutAccents.lowercase(Locale.getDefault()).trim()
}

private fun colorForVolunteer(name: String): Color {
    val index = kotlin.math.abs(name.hashCode()) % AvatarPalette.size
    return AvatarPalette[index]
}
