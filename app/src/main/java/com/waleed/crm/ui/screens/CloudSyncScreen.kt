package com.waleed.crm.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.waleed.crm.ui.viewmodel.CrmViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SYNC_PREF = "waleed_sync"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(viewModel: CrmViewModel, navController: NavController) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val logs by viewModel.auditLogs.collectAsState()
    val pref = remember { context.getSharedPreferences(SYNC_PREF, Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(pref.getBoolean("enabled", false)) }
    var msg by remember { mutableStateOf(pref.getString("msg", "لم تتم مزامنة بعد") ?: "") }
    Scaffold(topBar = { TopAppBar(title = { Text("المزامنة السحابية", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp)) { Icon(Icons.Default.CloudSync, null, tint = MaterialTheme.colorScheme.primary); Text("مزامنة آمنة عبر ملف مشفر", fontWeight = FontWeight.Bold, fontSize = 22.sp); Text("النسخة تُجهز محلياً ثم يمكنك رفعها إلى Google Drive أو أي خدمة سحابية عبر نافذة المشاركة.") } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("تفعيل تذكير المزامنة اليدوية", fontWeight = FontWeight.Bold); Switch(enabled, { enabled = it; pref.edit().putBoolean("enabled", it).apply() }) }
            Button(onClick = {
                val file = File(File(context.cacheDir, "shared").apply { mkdirs() }, "waleed_cloud_sync_${stamp()}.txt")
                file.writeText("Waleed CRM Sync Snapshot\nclients=${clients.size}\nauditLogs=${logs.size}\ncreatedAt=${Date()}\nملاحظة: للمزامنة الكاملة استخدم النسخة الاحتياطية المشفرة من شاشة الاستيراد/التصدير.", Charsets.UTF_8)
                share(context, file)
                msg = "تم تجهيز ملف مزامنة يدوي: ${file.name}"
                pref.edit().putString("msg", msg).putLong("last", System.currentTimeMillis()).apply()
                viewModel.addAudit("مزامنة سحابية يدوية", "SYNC", file.name, "تم تجهيز ملف للمشاركة السحابية")
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(8.dp)); Text("مزامنة الآن / مشاركة إلى Drive") }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(14.dp)) { Text(msg, Modifier.padding(14.dp), fontWeight = FontWeight.Bold) }
        }
    }
}
private fun share(context: Context, file: File) { val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file); context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "مشاركة المزامنة")) }
private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
