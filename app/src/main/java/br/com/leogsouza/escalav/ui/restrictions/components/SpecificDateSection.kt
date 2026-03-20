package br.com.leogsouza.escalav.ui.restrictions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leogsouza.escalav.data.remote.dto.PositionDto
import br.com.leogsouza.escalav.data.remote.dto.SpecificDateDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificDateSection(
    dates: List<SpecificDateDto>,
    positions: List<PositionDto>,
    scheduleStartDate: String?,
    scheduleEndDate: String?,
    onAdd: (SpecificDateDto) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Existing dates list
        dates.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatShortDate(entry.date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827)
                    )
                    val positionText = if (entry.positionId != null) {
                        positions.find { it.id == entry.positionId }?.name ?: "Posição #${entry.positionId}"
                    } else "Todas as posições"
                    Text(
                        text = positionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                    if (!entry.notes.isNullOrBlank()) {
                        Text(
                            text = entry.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remover",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (index < dates.lastIndex) {
                HorizontalDivider(color = Color(0xFFF3F4F6))
            }
        }

        if (dates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add button
        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFF9FAFB),
                contentColor = Color(0xFF374151)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = " Adicionar data",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    // Date picker dialog
    if (showDialog) {
        AddSpecificDateDialog(
            positions = positions,
            scheduleStartDate = scheduleStartDate,
            scheduleEndDate = scheduleEndDate,
            onDismiss = { showDialog = false },
            onConfirm = { entry ->
                onAdd(entry)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSpecificDateDialog(
    positions: List<PositionDto>,
    scheduleStartDate: String?,
    scheduleEndDate: String?,
    onDismiss: () -> Unit,
    onConfirm: (SpecificDateDto) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    var selectedPositionId by remember { mutableIntStateOf(-1) } // -1 = all positions
    var positionDropdownExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis ?: return@TextButton
                    val dateStr = Instant.ofEpochMilli(selectedMillis)
                        .atOffset(ZoneOffset.UTC)
                        .toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    onConfirm(
                        SpecificDateDto(
                            date = dateStr,
                            positionId = if (selectedPositionId == -1) null else selectedPositionId,
                            notes = notes.ifBlank { null }
                        )
                    )
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        Column {
            DatePicker(state = datePickerState)

            // Position picker
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = positionDropdownExpanded,
                    onExpandedChange = { positionDropdownExpanded = it }
                ) {
                    val selectedLabel = if (selectedPositionId == -1) "Todas as posições"
                    else positions.find { it.id == selectedPositionId }?.name ?: "Selecione"

                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Posição (opcional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = positionDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = positionDropdownExpanded,
                        onDismissRequest = { positionDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas as posições") },
                            onClick = { selectedPositionId = -1; positionDropdownExpanded = false }
                        )
                        positions.forEach { pos ->
                            DropdownMenuItem(
                                text = { Text(pos.name) },
                                onClick = { selectedPositionId = pos.id; positionDropdownExpanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

private fun formatShortDate(raw: String): String {
    return try {
        val locale = Locale("pt", "BR")
        val date = LocalDate.parse(raw.substring(0, 10))
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        date.format(fmt)
    } catch (_: Exception) {
        raw.take(10)
    }
}
