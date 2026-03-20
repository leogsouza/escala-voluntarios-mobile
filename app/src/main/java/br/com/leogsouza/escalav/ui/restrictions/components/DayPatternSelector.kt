package br.com.leogsouza.escalav.ui.restrictions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class DayPattern(val key: String, val label: String, val description: String)

private val DayPatterns = listOf(
    DayPattern("even", "Semanas pares", "2ª, 4ª semana do mês"),
    DayPattern("odd", "Semanas ímpares", "1ª, 3ª semana do mês"),
    DayPattern("last_week", "Última semana", "Última semana do mês"),
    DayPattern("first_week", "Primeira semana", "Primeira semana do mês")
)

@Composable
fun DayPatternSelector(
    selectedPattern: String,
    onPatternChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        DayPatterns.forEach { pattern ->
            val isSelected = selectedPattern == pattern.key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = {
                            // Tap-to-deselect: if already selected, clear selection
                            if (isSelected) {
                                onPatternChange("")
                            } else {
                                onPatternChange(pattern.key)
                            }
                        },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null // handled by selectable
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = pattern.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF111827)
                    )
                    Text(
                        text = pattern.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}
