package com.waleed.crm.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.waleed.crm.data.AuditLog
import com.waleed.crm.ui.viewmodel.CrmViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(viewModel: CrmViewModel, navController: NavController) {
    val context = LocalContext.current
    val logs by viewModel.auditLogs.collectAsState()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.refreshAuditLogs() }
    val filtered = logs.filter { query.isBlank() || it.action.contains(query, true) || it.entityName.contains(query, true) || it.entityType.contains(query, true) }
    Scaffold(topBar = { TopAppBar(title = { Text("سجل النشاط", fontWeight = FontWeight.Bold) }, actions = { IconButton(onClick = { shareCsv(context, filtered) }) { Icon(Icons.Default.IosShare, null) } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("بحث في السجل") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.History, null) })
            Spacer(Modifier.height(8.dp))
            LazyColumn { items(filtered, key = { it.id }) { LogCard(it) } }
        }
    }
}
@Composable private fun LogCard(log: AuditLog) { Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text(log.action, fontWeight = FontWeight.Bold); Text("${log.entityType} - ${log.entityName}"); Text(format(log.createdAt), color = MaterialTheme.colorScheme.outline); if (log.details.isNotBlank()) Text(log.details) } } }
private fun shareCsv(context: Context, logs: List<AuditLog>) { val file = File(File(context.cacheDir, "shared").apply { mkdirs() }, "audit_logs_${System.currentTimeMillis()}.csv"); file.writeText(buildString { appendLine("id,username,action,entity_type,entity_name,details,created_at"); logs.forEach { appendLine(listOf(it.id,it.username,it.action,it.entityType,it.entityName,it.details,it.createdAt).joinToString(",") { v -> "\"${v.toString().replace("\"", "\"\"")}\"" }) } }, Charsets.UTF_8); val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file); context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "تصدير سجل النشاط")) }
private fun format(t: Long) = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(t))
