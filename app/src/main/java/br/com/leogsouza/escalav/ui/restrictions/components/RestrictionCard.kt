package br.com.leogsouza.escalav.ui.restrictions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leogsouza.escalav.data.remote.dto.RestrictionDto
import java.util.Locale

private val CardBorder = Color(0xFFE5E7EB)
private val ActiveBadgeBg = Color(0xFFD1FAE5)
private val ActiveBadgeColor = Color(0xFF059669)
private val InactiveBadgeBg = Color(0xFFF3F4F6)
private val InactiveBadgeColor = Color(0xFF9CA3AF)
private val FixedPinColor = Color(0xFFF59E0B)
private val TypeBadgeBg = Color(0xFFF3F4F6)
private val TypeBadgeColor = Color(0xFF374151)
private val DeleteColor = Color(0xFFEF4444)

private val AvatarPalette = listOf(
    Color(0xFF3B82F6),
    Color(0xFFA855F7),
    Color(0xFFEC4899),
    Color(0xFFFB7185),
    Color(0xFFF97316),
    Color(0xFF06B6D4)
)

private fun colorForName(name: String): Color {
    val index = kotlin.math.abs(name.hashCode()) % AvatarPalette.size
    return AvatarPalette[index]
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "--"
        parts.size == 1 -> parts.first().take(2).uppercase(Locale.getDefault())
        else -> "${parts.first().first()}${parts.last().first()}".uppercase(Locale.getDefault())
    }
}

@Composable
fun RestrictionCard(
    restriction: RestrictionDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = restriction.volunteer?.name?.ifBlank { null }
        ?: restriction.volunteer?.fullName
        ?: "Voluntário #${restriction.volunteerId}"
    val roleName = restriction.volunteer?.mainRole?.name
    val avatarColor = colorForName(displayName)
    val isActive = restriction.active != false
    val isFixed = restriction.fixed == true
    val typeName = restriction.restrictionType?.name

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = SolidColor(CardBorder)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: avatar + name/role + pin + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials(displayName),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                    if (roleName != null) {
                        Text(
                            text = roleName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                if (isFixed) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Fixado",
                        tint = FixedPinColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Excluir",
                        tint = DeleteColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Type badge
                if (typeName != null) {
                    Surface(color = TypeBadgeBg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = typeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TypeBadgeColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                // Active/inactive badge
                val (activeBg, activeColor, activeLabel) = if (isActive) {
                    Triple(ActiveBadgeBg, ActiveBadgeColor, "Ativa")
                } else {
                    Triple(InactiveBadgeBg, InactiveBadgeColor, "Inativa")
                }
                Surface(color = activeBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = activeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = activeColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rules summary
            RulesSummary(rulesJson = restriction.rulesJson)

            if (!restriction.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = restriction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    maxLines = 2
                )
            }
        }
    }
}
