package br.com.leogsouza.escalav.ui.restrictions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.RestrictionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    onNewRestriction: () -> Unit,
    onEditRestriction: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: RestrictionsViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    var deleteTarget by remember { mutableStateOf<RestrictionDto?>(null) }

    LaunchedEffect(Unit) { viewModel.loadRestrictions() }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir restrição?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    target.id?.let { viewModel.deleteRestriction(it) }
                    deleteTarget = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas Restrições") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewRestriction) {
                Icon(Icons.Filled.Add, contentDescription = "Nova restrição")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                state.restrictions.isEmpty() -> Text("Nenhuma restrição cadastrada.", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.restrictions, key = { it.id ?: 0 }) { restriction ->
                        RestrictionListItem(
                            restriction = restriction,
                            onEdit = { restriction.id?.let(onEditRestriction) },
                            onDelete = { deleteTarget = restriction }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestrictionListItem(
    restriction: RestrictionDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(restriction.description.ifBlank { "Sem descrição" }, style = MaterialTheme.typography.bodyLarge)
                restriction.restrictionType?.let {
                    Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
