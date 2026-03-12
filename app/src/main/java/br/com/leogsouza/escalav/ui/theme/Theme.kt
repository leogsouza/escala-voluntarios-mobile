package br.com.leogsouza.escalav.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF1E3A5F)
private val PrimaryContainer = Color(0xFFD6E4FF)
private val Secondary = Color(0xFF3D6B9E)

private val LightColors = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary
)

@Composable
fun EscalaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
