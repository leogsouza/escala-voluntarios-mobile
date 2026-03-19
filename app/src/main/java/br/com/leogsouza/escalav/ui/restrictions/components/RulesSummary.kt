package br.com.leogsouza.escalav.ui.restrictions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

private val ExcludeModeColor = Color(0xFFEF4444)
private val IncludeModeColor = Color(0xFF3B82F6)
private val ExcludeBadgeBg = Color(0xFFFEE2E2)
private val IncludeBadgeBg = Color(0xFFDBEAFE)

private val DayPatternLabels = mapOf(
    "even" to "Semanas pares",
    "odd" to "Semanas ímpares",
    "last_week" to "Última semana",
    "first_week" to "Primeira semana"
)

/**
 * Parses rulesJson and returns (modeLabel, conditionsSummary).
 * E.g.: ("Não pode", "DM, DN · 3 datas específicas")
 */
fun parseRulesSummaryText(rulesJson: String?): Pair<String, String> {
    if (rulesJson.isNullOrBlank()) return "Sem regras" to ""
    return try {
        val obj = JSONObject(rulesJson)
        val mode = obj.optString("mode", "exclude")
        val modeLabel = if (mode == "include") "Pode" else "Não pode"

        val parts = mutableListOf<String>()

        // Service codes
        val codesArr = obj.optJSONArray("serviceCodes")
        if (codesArr != null && codesArr.length() > 0) {
            val codes = (0 until codesArr.length()).map { codesArr.getString(it) }
            parts.add(codes.joinToString(", "))
        }

        // Day pattern
        val pattern = obj.optString("dayPattern", "")
        if (pattern.isNotBlank()) {
            parts.add(DayPatternLabels[pattern] ?: pattern)
        }

        // Specific dates
        val datesArr = obj.optJSONArray("specificDates")
        if (datesArr != null && datesArr.length() > 0) {
            val n = datesArr.length()
            parts.add(if (n == 1) "1 data específica" else "$n datas específicas")
        }

        // Date ranges
        val rangesArr = obj.optJSONArray("dateRanges")
        if (rangesArr != null && rangesArr.length() > 0) {
            val n = rangesArr.length()
            parts.add(if (n == 1) "1 período" else "$n períodos")
        }

        val summary = if (parts.isEmpty()) "Sem condições" else parts.joinToString(" · ")
        modeLabel to summary
    } catch (_: Exception) {
        "Sem regras" to ""
    }
}

@Composable
fun RulesSummary(
    rulesJson: String?,
    modifier: Modifier = Modifier
) {
    if (rulesJson.isNullOrBlank()) {
        Text(
            text = "Sem regras definidas",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
            modifier = modifier
        )
        return
    }

    val (modeLabel, conditionsSummary) = parseRulesSummaryText(rulesJson)
    val isInclude = modeLabel == "Pode"
    val badgeBg = if (isInclude) IncludeBadgeBg else ExcludeBadgeBg
    val badgeColor = if (isInclude) IncludeModeColor else ExcludeModeColor

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = modeLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        if (conditionsSummary.isNotBlank()) {
            Text(
                text = conditionsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                maxLines = 2
            )
        }
    }
}
