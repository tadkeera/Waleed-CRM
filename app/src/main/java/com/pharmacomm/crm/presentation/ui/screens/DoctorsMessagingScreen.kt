package com.pharmacomm.crm.presentation.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.ClientType
import com.pharmacomm.crm.presentation.ui.components.FilterChipRow
import com.pharmacomm.crm.utils.AppContainer
import com.pharmacomm.crm.utils.WhatsAppBulkSender
import kotlinx.coroutines.launch

@Composable
fun DoctorsMessagingScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var clients by remember { mutableStateOf(listOf<Client>()) }
    var filteredClients by remember { mutableStateOf(listOf<Client>()) }
    var selectedSpecialty by remember { mutableStateOf<String?>(null) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var messageTemplate by remember { mutableStateOf("مرحباً د. {name}،\n\nنود إعلامك بأحدث العروض من شركتنا.\n\nمع خالص التحية،\nفريق PharmaComm") }
    var isSending by remember { mutableStateOf(false) }
    var selectedClients by remember { mutableStateOf(setOf<Long>()) }

    // Load data
    LaunchedEffect(Unit) {
        AppContainer.clientRepository.getAllClients().collect { list ->
            clients = list.filter { it.clientType == ClientType.DOCTOR }
            filteredClients = clients
        }
    }

    // Apply filters
    LaunchedEffect(clients, selectedSpecialty, selectedRegion, selectedClass) {
        filteredClients = clients.filter { client ->
            (selectedSpecialty == null || client.specialty == selectedSpecialty) &&
            (selectedRegion == null || client.region == selectedRegion) &&
            (selectedClass == null || client.importanceClass.name == selectedClass)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            "دليل الأطباء + الرسائل الجماعية",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        // Filters
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("الفلاتر", style = MaterialTheme.typography.titleMedium)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    FilterChipRow(
                        label = "التخصص",
                        options = clients.mapNotNull { it.specialty }.distinct(),
                        selected = selectedSpecialty,
                        onSelect = { selectedSpecialty = it }
                    )
                    FilterChipRow(
                        label = "المنطقة",
                        options = clients.mapNotNull { it.region }.distinct(),
                        selected = selectedRegion,
                        onSelect = { selectedRegion = it }
                    )
                    FilterChipRow(
                        label = "الكلاس",
                        options = listOf("CLASS_A", "CLASS_B", "CLASS_C"),
                        selected = selectedClass,
                        onSelect = { selectedClass = it }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Message Template
        OutlinedTextField(
            value = messageTemplate,
            onValueChange = { messageTemplate = it },
            label = { Text("نص الرسالة (استخدم {name})") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(140.dp),
            maxLines = 6
        )

        Spacer(Modifier.height(8.dp))

        // Bulk action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${filteredClients.size} طبيب محدد", style = MaterialTheme.typography.bodyMedium)

            Button(
                onClick = {
                    if (filteredClients.isEmpty()) {
                        Toast.makeText(context, "لا يوجد أطباء مطابقين", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSending = true
                    scope.launch {
                        val toSend = if (selectedClients.isNotEmpty()) {
                            filteredClients.filter { it.id in selectedClients }
                        } else filteredClients

                        WhatsAppBulkSender.sendBulkMessages(
                            context = context,
                            clients = toSend,
                            template = messageTemplate
                        ) { success ->
                            isSending = false
                            if (success) {
                                Toast.makeText(context, "تم بدء حملة الرسائل الجماعية!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("جارٍ الإرسال...")
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إرسال رسائل جماعية (${filteredClients.size})")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Doctors List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredClients) { doctor ->
                DoctorListItem(
                    doctor = doctor,
                    isSelected = selectedClients.contains(doctor.id),
                    onToggle = {
                        selectedClients = if (selectedClients.contains(doctor.id)) {
                            selectedClients - doctor.id
                        } else {
                            selectedClients + doctor.id
                        }
                    },
                    onSendSingle = {
                        WhatsAppBulkSender.sendSingleMessage(context, doctor, messageTemplate)
                    }
                )
            }
        }
    }
}

@Composable
fun DoctorListItem(
    doctor: Client,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onSendSingle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(doctor.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${doctor.specialty ?: ""} • ${doctor.region ?: ""} • ${doctor.importanceClass.displayName}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = onSendSingle, modifier = Modifier.padding(start = 8.dp)) {
                Text("إرسال")
            }
        }
    }
}