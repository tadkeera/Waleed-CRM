package com.waleed.crm.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.waleed.crm.data.Client
import com.waleed.crm.data.FollowUp
import com.waleed.crm.data.MessageLog
import com.waleed.crm.ui.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsScreen(viewModel: CrmViewModel, navController: NavController, clientId: Long) {
    val context = LocalContext.current
    var client by remember { mutableStateOf<Client?>(null) }
    var logs by remember { mutableStateOf<List<MessageLog>>(emptyList()) }
    var followUps by remember { mutableStateOf<List<FollowUp>>(emptyList()) }
    var followTitle by remember { mutableStateOf("متابعة الطبيب") }
    var followNotes by remember { mutableStateOf("") }

    fun reload() {
        viewModel.getClientById(clientId) { client = it }
        viewModel.getMessageLogsByClientId(clientId) { logs = it }
        viewModel.getFollowUpsByClientId(clientId) { followUps = it }
    }
    LaunchedEffect(clientId) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل العميل", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "رجوع") } },
                actions = {
                    IconButton(onClick = { navController.navigate("add_edit_client/$clientId?phone=") }) { Icon(Icons.Default.Edit, contentDescription = "تعديل") }
                    IconButton(onClick = { viewModel.deleteClient(clientId) { navController.popBackStack() } }) { Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error) }
                }
            )
        }
    ) { padding ->
        val c = client
        if (c == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(18.dp)) {
                            Text(c.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("${c.clientType} • Class ${c.clientClass}")
                            if (c.specialization.isNotBlank()) Text(c.specialization)
                            if (c.location.isNotBlank()) Text(c.location)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}"))) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(6.dp)); Text("اتصال") }
                        Button(onClick = { val clean = c.phone.replace("+", "").replace(" ", "").replace("-", ""); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$clean"))) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Message, null); Spacer(Modifier.width(6.dp)); Text("واتساب") }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            InfoLine("الهاتف", c.phone)
                            if (c.secondPhone.isNotBlank()) InfoLine("هاتف إضافي", c.secondPhone)
                            InfoLine("تاريخ الإضافة", formatDate(c.dateAdded))
                            InfoLine("آخر تعديل", formatDate(c.updatedAt))
                            InfoLine("الملاحظات", c.notes.ifBlank { "لا توجد ملاحظات" })
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Event, null); Spacer(Modifier.width(8.dp)); Text("إضافة متابعة", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(followTitle, { followTitle = it }, label = { Text("عنوان المتابعة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(followNotes, { followNotes = it }, label = { Text("ملاحظات المتابعة") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                FollowButton("غداً", 1, clientId, followTitle, followNotes, viewModel) { followNotes = ""; reload() }
                                FollowButton("بعد أسبوع", 7, clientId, followTitle, followNotes, viewModel) { followNotes = ""; reload() }
                                FollowButton("بعد شهر", 30, clientId, followTitle, followNotes, viewModel) { followNotes = ""; reload() }
                            }
                        }
                    }
                }
                item { Text("المتابعات", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                if (followUps.isEmpty()) item { Text("لا توجد متابعات لهذا العميل.", color = MaterialTheme.colorScheme.outline) } else items(followUps, key = { it.id }) { f ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(f.title, fontWeight = FontWeight.Bold); Text(formatDate(f.dueAt), fontSize = 13.sp); if (f.notes.isNotBlank()) Text(f.notes, fontSize = 13.sp) }
                            AssistChip(onClick = {}, label = { Text(if (f.status == "DONE") "تم" else "معلق") })
                        }
                    }
                }
                item { Text("سجل التواصل", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                if (logs.isEmpty()) item { Text("لا يوجد سجل تواصل لهذا العميل حتى الآن.", color = MaterialTheme.colorScheme.outline) } else items(logs, key = { it.id }) { log ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp)) { Text(formatDate(log.timestamp), fontWeight = FontWeight.Bold); Text("الطريقة: ${modeLabelDetails(log.sendMode)} • الحالة: ${log.status}", fontSize = 13.sp); if (log.messageText.isNotBlank()) Text(log.messageText, maxLines = 3, fontSize = 13.sp); if (log.attachmentName.isNotBlank()) Text("المرفق: ${log.attachmentName}", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable private fun RowScope.FollowButton(label: String, days: Int, clientId: Long, title: String, notes: String, viewModel: CrmViewModel, after: () -> Unit) {
    OutlinedButton(onClick = {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
        viewModel.addFollowUp(clientId, title.ifBlank { "متابعة" }, cal.timeInMillis, notes, after)
    }, modifier = Modifier.weight(1f)) { Text(label, fontSize = 12.sp) }
}
@Composable private fun InfoLine(label: String, value: String) { Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline); Text(value, fontWeight = FontWeight.Medium) } }
private fun formatDate(time: Long): String = try { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(time)) } catch (_: Exception) { "-" }
private fun modeLabelDetails(mode: String): String = when (mode) { "TEXT_ONLY" -> "نص فقط"; "ATTACHMENT_ONLY" -> "مرفق فقط"; "TEXT_AND_ATTACHMENT" -> "نص مع مرفق"; else -> mode }
