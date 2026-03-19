package br.com.leogsouza.escalav.ui.restrictions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leogsouza.escalav.data.remote.dto.ServiceCodeDto

private val ChipSelectedBg = Color(0xFFDBEAFE)
private val ChipSelectedBorder = Color(0xFF3B82F6)
private val ChipSelectedText = Color(0xFF1D4ED8)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceCodeChips(
    availableCodes: List<ServiceCodeDto>,
    selectedCodes: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableCodes.isEmpty()) {
        Text(
            text = "Nenhum código de serviço disponível",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
            modifier = modifier
        )
        return
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        availableCodes.forEach { serviceCode ->
            val isSelected = serviceCode.code in selectedCodes
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(serviceCode.code) },
                label = {
                    Text(
                        text = serviceCode.namePt,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) ChipSelectedText else Color(0xFF374151),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChipSelectedBg,
                    containerColor = Color(0xFFF9FAFB)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = ChipSelectedBorder,
                    borderColor = Color(0xFFD1D5DB)
                )
            )
        }
    }
}
