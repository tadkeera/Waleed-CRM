package com.pharmacomm.crm.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.ClientType
import com.pharmacomm.crm.domain.model.ImportanceClass
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClientCard(
    client: Client,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = when (client.specialty?.lowercase()) {
        "قلب" -> Color(0xFFE53935).copy(alpha = 0.15f)
        "أعصاب" -> Color(0xFF8E24AA).copy(alpha = 0.15f)
        "أطفال" -> Color(0xFF1E88E5).copy(alpha = 0.15f)
        "جلدية" -> Color(0xFF43A047).copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = client.clientType.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Class badge
                val classColor = when (client.importanceClass) {
                    ImportanceClass.CLASS_A -> Color(0xFF4CAF50)
                    ImportanceClass.CLASS_B -> Color(0xFFFF9800)
                    ImportanceClass.CLASS_C -> Color(0xFF9E9E9E)
                    else -> Color.Gray
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = classColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = client.importanceClass.displayName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = classColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Specialty & Region
            if (!client.specialty.isNullOrBlank() || !client.region.isNullOrBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!client.specialty.isNullOrBlank()) {
                        Chip(text = "🩺 ${client.specialty}")
                    }
                    if (!client.region.isNullOrBlank()) {
                        Chip(text = "📍 ${client.region}")
                    }
                }
            }

            // Last contact info
            client.lastContactDate?.let { date ->
                val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
                Text(
                    text = "آخر تواصل: ${formatter.format(date)} (${client.contactCount} مرات)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Phones count and actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "أرقام: ${client.id}", // placeholder
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "حذف", 
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}