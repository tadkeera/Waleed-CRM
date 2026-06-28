package com.pharmacomm.crm.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.ImportanceClass
import com.pharmacomm.crm.utils.AppContainer
import kotlinx.coroutines.flow.collectLatest
import java.util.*

@Composable
fun DashboardScreen() {
    var totalClients by remember { mutableIntStateOf(0) }
    var completedClients by remember { mutableIntStateOf(0) }
    var incompleteClients by remember { mutableStateOf(listOf<Client>()) }
    var weeklyContacts by remember { mutableIntStateOf(0) }
    var dormancyAlerts by remember { mutableStateOf(listOf<Client>()) }

    LaunchedEffect(Unit) {
        // Collect stats
        AppContainer.clientRepository.getTotalClientsCount().collectLatest {
            totalClients = it
        }
    }
    LaunchedEffect(Unit) {
        AppContainer.clientRepository.getCompletedClientsCount().collectLatest {
            completedClients = it
        }
    }
    LaunchedEffect(Unit) {
        AppContainer.clientRepository.getIncompleteClients().collectLatest {
            incompleteClients = it
        }
    }

    // Weekly contacts
    LaunchedEffect(Unit) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val weekStart = cal.timeInMillis
        AppContainer.contactLogRepository.getTotalWeeklyContacts(weekStart).collectLatest {
            weeklyContacts = it
        }
    }

    // Dormancy alerts
    LaunchedEffect(Unit) {
        AppContainer.clientRepository.getAllClients().collectLatest { clients ->
            val now = System.currentTimeMillis()
            val weekAgo = now - (7 * 24 * 60 * 60 * 1000)
            val twoWeeksAgo = now - (14 * 24 * 60 * 60 * 1000)
            val monthAgo = now - (30 * 24 * 60 * 60 * 1000)

            dormancyAlerts = clients.filter { client ->
                val lastContact = client.lastContactDate?.time ?: 0
                when (client.importanceClass) {
                    ImportanceClass.CLASS_A -> lastContact < weekAgo
                    ImportanceClass.CLASS_B -> lastContact < twoWeeksAgo
                    ImportanceClass.CLASS_C -> lastContact < monthAgo
                    else -> false
                }
            }.take(8)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("لوحة التحكم التحليلية", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        // KPI Cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("إجمالي العملاء", totalClients.toString(), MaterialTheme.colorScheme.primary)
            StatCard("مكتملون 100%", completedClients.toString(), Color(0xFF4CAF50))
            StatCard("تواصل أسبوعي", weeklyContacts.toString(), Color(0xFF2196F3))
        }

        Spacer(Modifier.height(20.dp))

        // Incomplete clients
        Text("العملاء الذين يحتاجون استكمال بياناتهم", style = MaterialTheme.typography.titleMedium)
        if (incompleteClients.isEmpty()) {
            Text("جميع العملاء مكتملون ✅", color = Color(0xFF4CAF50))
        } else {
            LazyColumn(modifier = Modifier.height(150.dp)) {
                items(incompleteClients.take(5)) { client ->
                    Text("• ${client.name} - ${client.clientType.displayName}")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Dormancy Alerts
        Text("تنبيهات الانقطاع (${dormancyAlerts.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(dormancyAlerts) { client ->
                DormancyAlertRow(client)
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineLarge, color = color, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DormancyAlertRow(client: Client) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (client.importanceClass) {
                ImportanceClass.CLASS_A -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(client.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${client.importanceClass.displayName} • ${client.specialty ?: ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                when (client.importanceClass) {
                    ImportanceClass.CLASS_A -> "تواصل أسبوعي مطلوب"
                    ImportanceClass.CLASS_B -> "تواصل كل أسبوعين"
                    else -> "تواصل شهري مطلوب"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.Red
            )
        }
    }
}