package br.com.leogsouza.escalav.ui.schedule

import androidx.compose.ui.graphics.Color
import br.com.leogsouza.escalav.data.remote.dto.EventDto
import java.text.Normalizer
import java.util.Locale

// private implementation detail — callers only need the colour functions below
private enum class EventVisualType { RJM, ENSAIO, CULTO, DEFAULT }

/** Strips accents and lower-cases [value] for fuzzy matching. */
internal fun normalizeAccents(value: String): String {
    val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
    return withoutAccents.lowercase(Locale.getDefault()).trim()
}

private fun eventVisualType(event: EventDto): EventVisualType {
    val code = normalizeAccents(event.serviceCode)
    if (code.contains("rjm")) return EventVisualType.RJM
    if (code.contains("ensaio") || code == "el") return EventVisualType.ENSAIO
    if (code.contains("culto") || code == "dn") return EventVisualType.CULTO
    val service = normalizeAccents(event.service)
    return when {
        service.contains("rjm") -> EventVisualType.RJM
        service.contains("ensaio") -> EventVisualType.ENSAIO
        service.contains("culto") -> EventVisualType.CULTO
        else -> EventVisualType.DEFAULT
    }
}

/** Full gradient colors used in the event detail / day detail headers. */
internal fun headerGradientColors(event: EventDto): List<Color> = when (eventVisualType(event)) {
    EventVisualType.RJM -> listOf(Color(0xFF8A2BE2), Color(0xFF6A0DAD))
    EventVisualType.ENSAIO -> listOf(Color(0xFFFF8A00), Color(0xFFFF6A00))
    EventVisualType.CULTO -> listOf(Color(0xFF3B5BDB), Color(0xFF2E4FD6))
    EventVisualType.DEFAULT -> listOf(Color(0xFF4B5563), Color(0xFF374151))
}

/** (background, text) tints for the small pill shown inside a calendar day cell. */
internal fun eventPillColors(event: EventDto): Pair<Color, Color> = when (eventVisualType(event)) {
    EventVisualType.RJM -> Color(0xFFF3E8FF) to Color(0xFF7C3AED)
    EventVisualType.ENSAIO -> Color(0xFFFFF7ED) to Color(0xFFEA580C)
    EventVisualType.CULTO -> Color(0xFFEFF6FF) to Color(0xFF2563EB)
    EventVisualType.DEFAULT -> Color(0xFFF3F4F6) to Color(0xFF374151)
}
