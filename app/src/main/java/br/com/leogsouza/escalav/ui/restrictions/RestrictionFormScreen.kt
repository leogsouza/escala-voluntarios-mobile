package br.com.leogsouza.escalav.ui.restrictions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.RestrictionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionFormScreen(
    restrictionId: Int?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RestrictionsViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    var description by remember { mutableStateOf("") }
    var selectedTypeId by remember { mutableStateOf<Int?>(null) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(restrictionId) { viewModel.loadFormData(restrictionId) }

    LaunchedEffect(formState.restriction) {
        formState.restriction?.let {
            description = it.description
            selectedTypeId = it.restrictionTypeId
        }
    }

    LaunchedEffect(formState.saved) {
        if (formState.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (restrictionId == null) "Nova Restrição" else "Editar Restrição") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (formState.loading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formState.restrictionTypes.find { it.id == selectedTypeId }?.name
                            ?: "Selecione o tipo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de restrição") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        formState.restrictionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedTypeId = type.id
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                formState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        val scheduleId = listState.scheduleId ?: return@Button
                        val volunteerId = listState.volunteerId ?: return@Button
                        val typeId = selectedTypeId ?: return@Button
                        viewModel.saveRestriction(
                            RestrictionDto(
                                id = formState.restriction?.id,
                                volunteerId = volunteerId,
                                scheduleId = scheduleId,
                                description = description.trim(),
                                restrictionTypeId = typeId,
                                restrictionType = null,
                                rulesJson = formState.restriction?.rulesJson,
                                active = true,
                                fixed = false
                            ),
                            restrictionId
                        )
                    },
                    enabled = description.isNotBlank() && selectedTypeId != null && !formState.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (formState.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (restrictionId == null) "Criar" else "Salvar")
                    }
                }
            }
        }
    }
}
