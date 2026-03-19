package br.com.leogsouza.escalav.ui.restrictions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.RestrictionDto
import br.com.leogsouza.escalav.ui.restrictions.components.RestrictionCard
import br.com.leogsouza.escalav.ui.restrictions.components.ScheduleBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    onNewRestriction: () -> Unit,
    onEditRestriction: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: RestrictionsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var deleteTarget by remember { mutableStateOf<RestrictionDto?>(null) }

    LaunchedEffect(Unit) { viewModel.loadInitialData() }

    // Delete confirmation dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir restrição?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    target.id?.let { viewModel.deleteRestriction(it) }
                    deleteTarget = null
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restrições") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewRestriction) {
                Icon(Icons.Filled.Add, contentDescription = "Nova restrição")
            }
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Schedule banner
            state.schedule?.let { schedule ->
                ScheduleBanner(
                    schedule = schedule,
                    itemCount = state.totalItems,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Search field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text("Buscar por nome ou descrição...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // Role filter chips
            if (state.roleCounts.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedRoleId == null,
                            onClick = { viewModel.filterByRole(null) },
                            label = { Text("Todos (${state.totalItems})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1E40AF),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(state.roleCounts) { roleCount ->
                        FilterChip(
                            selected = state.selectedRoleId == roleCount.roleId,
                            onClick = { viewModel.filterByRole(roleCount.roleId) },
                            label = { Text("${roleCount.roleName} (${roleCount.count})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1E40AF),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Content area
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )

                    state.error != null -> Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )

                    state.restrictions.isEmpty() -> Text(
                        text = if (state.searchQuery.isNotBlank()) "Nenhum resultado para \"${state.searchQuery}\"."
                        else "Nenhuma restrição cadastrada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.restrictions, key = { it.id ?: 0 }) { restriction ->
                            RestrictionCard(
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
}
