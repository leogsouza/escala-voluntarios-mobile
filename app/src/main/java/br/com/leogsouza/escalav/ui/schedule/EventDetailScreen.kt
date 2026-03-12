package br.com.leogsouza.escalav.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.AssignmentDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    onBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val state by viewModel.eventState.collectAsState()

    LaunchedEffect(eventId) { viewModel.loadEventDetail(eventId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.event?.service ?: "Evento") },
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
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.event?.let { event ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(event.service, style = MaterialTheme.typography.headlineSmall)
                                    Text("Data: ${event.date.substring(0, 10)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Horário: ${event.time}", style = MaterialTheme.typography.bodyMedium)
                                    if (event.notes.isNotBlank()) Text("Obs: ${event.notes}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        item {
                            Text(
                                "Voluntários escalados",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    items(state.assignments) { assignment ->
                        AssignmentCard(assignment)
                    }
                    if (state.assignments.isEmpty() && !state.loading) {
                        item { Text("Nenhum voluntário escalado.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentCard(assignment: AssignmentDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    assignment.volunteer?.fullName ?: assignment.volunteer?.name ?: "—",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    assignment.position?.name ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssignmentStatusChip(assignment.status)
        }
    }
}

@Composable
private fun AssignmentStatusChip(status: String) {
    val (label, containerColor) = when (status.uppercase()) {
        "APPROVED" -> "Aprovado" to MaterialTheme.colorScheme.primaryContainer
        "PENDING" -> "Pendente" to MaterialTheme.colorScheme.tertiaryContainer
        "REJECTED" -> "Rejeitado" to MaterialTheme.colorScheme.errorContainer
        else -> status to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = containerColor, shape = MaterialTheme.shapes.small) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}
