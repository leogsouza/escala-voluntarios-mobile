package br.com.leogsouza.escalav.ui.restrictions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leogsouza.escalav.data.remote.dto.ScheduleDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BannerBg = Color(0xFFEFF6FF)
private val BannerText = Color(0xFF1E40AF)
private val BannerSubtext = Color(0xFF3B82F6)

@Composable
fun ScheduleBanner(
    schedule: ScheduleDto?,
    itemCount: Int? = null,
    modifier: Modifier = Modifier
) {
    if (schedule == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BannerBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = BannerText,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BannerText
                )
                Text(
                    text = formatScheduleDateRange(schedule.startDate, schedule.endDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = BannerSubtext
                )
            }
        }
        if (itemCount != null) {
            Text(
                text = if (itemCount == 1) "1 restrição" else "$itemCount restrições",
                style = MaterialTheme.typography.labelSmall,
                color = BannerSubtext,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatScheduleDateRange(start: String, end: String): String {
    return try {
        val locale = Locale("pt", "BR")
        val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
        val s = LocalDate.parse(start.substring(0, 10))
        val e = LocalDate.parse(end.substring(0, 10))
        "${s.format(fmt)} – ${e.format(fmt)}, ${e.year}"
    } catch (_: Exception) {
        "${start.take(10)} – ${end.take(10)}"
    }
}
