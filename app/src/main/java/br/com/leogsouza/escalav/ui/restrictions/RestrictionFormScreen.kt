package br.com.leogsouza.escalav.ui.restrictions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leogsouza.escalav.data.remote.dto.DateRangeEntryDto
import br.com.leogsouza.escalav.data.remote.dto.SpecificDateDto
import br.com.leogsouza.escalav.ui.restrictions.components.DateRangeSection
import br.com.leogsouza.escalav.ui.restrictions.components.DayPatternSelector
import br.com.leogsouza.escalav.ui.restrictions.components.ScheduleBanner
import br.com.leogsouza.escalav.ui.restrictions.components.ServiceCodeChips
import br.com.leogsouza.escalav.ui.restrictions.components.SpecificDateSection
import br.com.leogsouza.escalav.ui.restrictions.components.VolunteerSearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionFormScreen(
    restrictionId: Int?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RestrictionsFormViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val volunteerResults by viewModel.volunteerResults.collectAsState()
    val volunteerSearching by viewModel.volunteerSearching.collectAsState()

    // Local form state
    var description by remember { mutableStateOf("") }
    var selectedTypeId by remember { mutableStateOf<Int?>(null) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    // Rules state (synced from formState when editing)
    var ruleMode by remember { mutableStateOf("exclude") }
    var selectedServiceCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var dayPattern by remember { mutableStateOf("") }
    var specificDates by remember { mutableStateOf<List<SpecificDateDto>>(emptyList()) }
    var dateRanges by remember { mutableStateOf<List<DateRangeEntryDto>>(emptyList()) }
    var isActive by remember { mutableStateOf(true) }
    var isFixed by remember { mutableStateOf(false) }

    LaunchedEffect(restrictionId) { viewModel.loadFormData(restrictionId) }

    // Populate local state when editing an existing restriction
    LaunchedEffect(formState.restriction) {
        formState.restriction?.let { r ->
            description = r.description
            selectedTypeId = r.restrictionTypeId
            isActive = r.active ?: true
            isFixed = r.fixed ?: false
        }
    }

    // Sync parsed rules from ViewModel when they are populated
    LaunchedEffect(formState.ruleMode, formState.selectedServiceCodes, formState.dayPattern,
        formState.specificDates, formState.dateRanges) {
        ruleMode = formState.ruleMode
        selectedServiceCodes = formState.selectedServiceCodes
        dayPattern = formState.dayPattern
        specificDates = formState.specificDates
        dateRanges = formState.dateRanges
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB)
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
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Schedule banner
            ScheduleBanner(
                schedule = formState.schedule,
                itemCount = null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // --- VOLUNTEER SECTION ---
            FormSection(title = "Voluntário") {
                VolunteerSearchField(
                    selectedVolunteer = formState.selectedVolunteer,
                    searchResults = volunteerResults,
                    onSearchQueryChange = { viewModel.searchVolunteers(it) },
                    onVolunteerSelected = { viewModel.selectVolunteer(it) },
                    onClear = { viewModel.clearVolunteer() },
                    isSearching = volunteerSearching,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- TYPE SECTION ---
            FormSection(title = "Tipo de restrição") {
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formState.restrictionTypes.find { it.id == selectedTypeId }?.name
                            ?: "Selecione o tipo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
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
            }

            // --- DESCRIPTION SECTION ---
            FormSection(title = "Descrição") {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Observações sobre esta restrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            // --- RULES SECTION ---
            FormSection(title = "Regras de disponibilidade") {
                // Mode toggle (exclude / include)
                Text(
                    text = "Modo",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = ruleMode == "include",
                        onClick = { ruleMode = "include" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Color(0xFFDBEAFE),
                            activeContentColor = Color(0xFF1D4ED8)
                        )
                    ) {
                        Text("PODE SERVIR")
                    }
                    SegmentedButton(
                        selected = ruleMode == "exclude",
                        onClick = { ruleMode = "exclude" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Color(0xFFFFE4E4),
                            activeContentColor = Color(0xFFB91C1C)
                        )
                    ) {
                        Text("NÃO PODE")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Service codes (from schedule's events)
                if (formState.serviceCodes.isNotEmpty()) {
                    Text(
                        text = "Cultos",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    ServiceCodeChips(
                        availableCodes = formState.serviceCodes,
                        selectedCodes = selectedServiceCodes,
                        onToggle = { code ->
                            selectedServiceCodes = if (code in selectedServiceCodes)
                                selectedServiceCodes - code
                            else
                                selectedServiceCodes + code
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Day pattern
                Text(
                    text = "Padrão de dias",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                DayPatternSelector(
                    selectedPattern = dayPattern,
                    onPatternChange = { dayPattern = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Specific dates
                Text(
                    text = "Datas específicas",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SpecificDateSection(
                    dates = specificDates,
                    positions = formState.positions,
                    scheduleStartDate = formState.schedule?.startDate,
                    scheduleEndDate = formState.schedule?.endDate,
                    onAdd = { specificDates = specificDates + it },
                    onRemove = { index -> specificDates = specificDates.toMutableList().also { it.removeAt(index) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date ranges
                Text(
                    text = "Intervalos de datas",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                DateRangeSection(
                    ranges = dateRanges,
                    positions = formState.positions,
                    scheduleStartDate = formState.schedule?.startDate,
                    scheduleEndDate = formState.schedule?.endDate,
                    onAdd = { dateRanges = dateRanges + it },
                    onRemove = { index -> dateRanges = dateRanges.toMutableList().also { it.removeAt(index) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- CONFIG SECTION ---
            FormSection(title = "Configurações") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ativo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "Restrição considerada nas escalas",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = Color(0xFFF3F4F6))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fixo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "Não pode ser substituído pelo algoritmo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Switch(checked = isFixed, onCheckedChange = { isFixed = it })
                }
            }

            // Error display
            formState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Save button
            Button(
                onClick = {
                    val volunteer = formState.selectedVolunteer ?: return@Button
                    val typeId = selectedTypeId ?: return@Button
                    viewModel.saveRestriction(
                        volunteerId = volunteer.id,
                        restrictionTypeId = typeId,
                        description = description,
                        ruleMode = ruleMode,
                        selectedServiceCodes = selectedServiceCodes,
                        dayPattern = dayPattern,
                        specificDates = specificDates,
                        dateRanges = dateRanges,
                        active = isActive,
                        fixed = isFixed,
                        restrictionId = restrictionId
                    )
                },
                enabled = formState.selectedVolunteer != null
                        && selectedTypeId != null
                        && description.isNotBlank()
                        && !formState.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
            ) {
                if (formState.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (restrictionId == null) "Criar restrição" else "Salvar alterações")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}
