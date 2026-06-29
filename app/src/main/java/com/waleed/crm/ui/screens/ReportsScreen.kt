package com.waleed.crm.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.waleed.crm.data.Client
import com.waleed.crm.data.FollowUpWithClient
import com.waleed.crm.data.MessageCampaign
import com.waleed.crm.data.MessageLog
import com.waleed.crm.ui.viewmodel.CrmViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: CrmViewModel, navController: NavController) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val campaigns by viewModel.messageCampaigns.collectAsState()
    val followUps by viewModel.followUps.collectAsState()
    var logs by remember { mutableStateOf<List<MessageLog>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.getAllMessageLogs { logs = it } }

    val doctors = clients.filter { it.clientType == "طبيب" }
    val overdue = followUps.count { it.followUp.dueAt < System.currentTimeMillis() }

    Scaffold(topBar = { TopAppBar(title = { Text("التقارير", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Assessment, null, tint = MaterialTheme.colorScheme.primary)
                    Text("تقارير Waleed CRM", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("ملخصات إدارية قابلة للتصدير والمشاركة بصيغة CSV وملف تقرير قابل للطباعة.")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatBox("الأطباء", doctors.size.toString(), Modifier.weight(1f))
                StatBox("الرسائل", logs.size.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatBox("الحملات", campaigns.size.toString(), Modifier.weight(1f))
                StatBox("متأخرة", overdue.toString(), Modifier.weight(1f))
            }
            ReportButton("تقرير شامل قابل للطباعة", "ملف HTML يفتح ويطبع كـ PDF", Icons.Default.PictureAsPdf) {
                val file = writeReportFile(context, "waleed_crm_report_${stamp()}.html", htmlReport(clients, logs, campaigns, followUps))
                shareFile(context, file, "text/html")
                message = "تم تجهيز التقرير الشامل للطباعة أو المشاركة."
            }
            ReportButton("تقرير الأطباء CSV", "الاسم، الهاتف، التخصص، المنطقة، التصنيف", Icons.Default.FileDownload) {
                shareFile(context, writeReportFile(context, "doctors_report_${stamp()}.csv", doctorsCsv(doctors)), "text/csv")
                message = "تم تجهيز تقرير الأطباء."
            }
            ReportButton("تقرير المتابعات CSV", "المتابعات القادمة والمتأخرة", Icons.Default.FileDownload) {
                shareFile(context, writeReportFile(context, "followups_report_${stamp()}.csv", followUpsCsv(followUps)), "text/csv")
                message = "تم تجهيز تقرير المتابعات."
            }
            if (message.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(14.dp)) { Text(message, Modifier.padding(14.dp), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun StatBox(label: String, value: String, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp); Text(label) } } }
@Composable private fun ReportButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { Button(onClick, Modifier.fillMaxWidth().heightIn(min = 72.dp), shape = RoundedCornerShape(16.dp)) { Icon(icon, null); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 12.sp) } } }
private fun htmlReport(clients: List<Client>, logs: List<MessageLog>, campaigns: List<MessageCampaign>, followUps: List<FollowUpWithClient>): String = """
<html dir='rtl'><head><meta charset='utf-8'><style>body{font-family:sans-serif;padding:24px}table{width:100%;border-collapse:collapse}td,th{border:1px solid #ddd;padding:8px}.card{background:#eef;padding:12px;border-radius:12px;margin:8px 0}</style></head><body>
<h1>Waleed CRM - تقرير شامل</h1><p>تاريخ الإنشاء: ${Date()}</p>
<div class='card'>إجمالي العملاء: ${clients.size} | الرسائل: ${logs.size} | الحملات: ${campaigns.size} | المتابعات المعلقة: ${followUps.size}</div>
<h2>أول 100 طبيب/عميل</h2><table><tr><th>الاسم</th><th>الهاتف</th><th>النوع</th><th>التخصص</th><th>المنطقة</th></tr>${clients.take(100).joinToString("") { "<tr><td>${it.name}</td><td>${it.phone}</td><td>${it.clientType}</td><td>${it.specialization}</td><td>${it.location}</td></tr>" }}</table>
</body></html>
""".trimIndent()
private fun doctorsCsv(items: List<Client>) = buildString { appendLine("name,phone,specialization,class,location,notes"); items.forEach { appendLine(listOf(it.name,it.phone,it.specialization,it.clientClass,it.location,it.notes).joinToString(",") { v -> csv(v) }) } }
private fun followUpsCsv(items: List<FollowUpWithClient>) = buildString { appendLine("title,client,due_at,status,notes"); items.forEach { appendLine(listOf(it.followUp.title, it.client?.name ?: "", it.followUp.dueAt.toString(), it.followUp.status, it.followUp.notes).joinToString(",") { v -> csv(v) }) } }
private fun writeReportFile(context: Context, name: String, content: String): File { val dir = File(context.cacheDir, "shared").apply { mkdirs() }; return File(dir, name).apply { writeText(content, Charsets.UTF_8) } }
private fun shareFile(context: Context, file: File, mime: String) { val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file); context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "مشاركة التقرير")) }
private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
private fun csv(v: String) = "\"" + v.replace("\"", "\"\"").replace("\n", " ") + "\""
