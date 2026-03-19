package br.com.leogsouza.escalav.ui.restrictions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import br.com.leogsouza.escalav.data.remote.dto.VolunteerDto

@Composable
fun VolunteerSearchField(
    selectedVolunteer: VolunteerDto?,
    searchResults: List<VolunteerDto>,
    onSearchQueryChange: (String) -> Unit,
    onVolunteerSelected: (VolunteerDto) -> Unit,
    onClear: () -> Unit,
    isSearching: Boolean = false,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        if (selectedVolunteer != null) {
            // Selected state — show chip with clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color(0xFF1E40AF),
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = selectedVolunteer.name.ifBlank { selectedVolunteer.fullName },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E40AF)
                    )
                    val roleName = selectedVolunteer.mainRole?.name
                    if (roleName != null) {
                        Text(
                            text = roleName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
                IconButton(onClick = {
                    query = ""
                    onClear()
                }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Remover",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // Search input state
            Box {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearchQueryChange(it)
                    },
                    label = { Text("Buscar voluntário") },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        }
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                query = ""
                                onSearchQueryChange("")
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpar")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Dropdown results
                if (searchResults.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp)
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .zIndex(10f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        Column {
                            searchResults.forEachIndexed { index, volunteer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onVolunteerSelected(volunteer)
                                            query = ""
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(
                                            text = volunteer.name.ifBlank { volunteer.fullName },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF111827)
                                        )
                                        val role = volunteer.mainRole?.name
                                        if (role != null) {
                                            Text(
                                                text = role,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        }
                                    }
                                }
                                if (index < searchResults.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF3F4F6))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
