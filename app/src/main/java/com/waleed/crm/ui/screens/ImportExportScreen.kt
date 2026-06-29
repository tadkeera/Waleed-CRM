package com.waleed.crm.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
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
import com.waleed.crm.data.MessageCampaign
import com.waleed.crm.data.MessageLog
import com.waleed.crm.ui.viewmodel.CrmViewModel
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(viewModel: CrmViewModel, navController: NavController) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val campaigns by viewModel.messageCampaigns.collectAsState()
    val templates by viewModel.messageTemplates.collectAsState()
    val followUps by viewModel.followUps.collectAsState()
    var message by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var pendingRestoreClients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var logs by remember { mutableStateOf<List<MessageLog>>(emptyList()) }

    LaunchedEffect(Unit) { viewModel.getAllMessageLogs { logs = it } }

    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "" }
            .onSuccess { csv -> viewModel.importClientsFromCsv(csv) { inserted, skipped -> message = "تم استيراد $inserted سجل، وتجاوز $skipped سجل مكرر/غير صالح." } }
            .onFailure { message = "تعذر قراءة ملف CSV." }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        if (restorePassword.length < 6) { message = "أدخل كلمة مرور النسخة أولاً."; return@rememberLauncherForActivityResult }
        runCatching {
            val encrypted = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val json = decryptText(encrypted, restorePassword)
            parseClientsFromBackup(json)
        }.onSuccess { parsed ->
            pendingRestoreClients = parsed
            message = "تمت قراءة النسخة بنجاح. تحتوي على ${parsed.size} طبيب/عميل. اختر الدمج أو الاستبدال."
        }.onFailure {
            pendingRestoreClients = emptyList()
            message = "فشل فتح النسخة: كلمة المرور غير صحيحة أو الملف تالف."
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("الاستيراد والتصدير والاستعادة", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "رجوع") } }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard()
            ExportButton("تصدير الأطباء والعملاء CSV", "ينشئ ملف قابل للفتح في Excel", Icons.Default.FileDownload) {
                shareFile(context, writeSharedFile(context, "clients_${stamp()}.csv", clientsToCsv(clients)), "text/csv")
                message = "تم تجهيز ملف العملاء للمشاركة."
            }
            ExportButton("تصدير سجل الرسائل CSV", "يشمل نص الرسالة والمرفق والحملة", Icons.Default.FileDownload) {
                shareFile(context, writeSharedFile(context, "message_logs_${stamp()}.csv", logsToCsv(logs)), "text/csv")
                message = "تم تجهيز سجل الرسائل للمشاركة."
            }
            ExportButton("تصدير تقرير الحملات CSV", "تقرير مبسط عن حملات واتساب", Icons.Default.Share) {
                shareFile(context, writeSharedFile(context, "campaigns_${stamp()}.csv", campaignsToCsv(campaigns)), "text/csv")
                message = "تم تجهيز تقرير الحملات للمشاركة."
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نسخ احتياطي مشفر", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedTextField(password, { password = it }, label = { Text("كلمة مرور النسخة الاحتياطية") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = {
                        if (password.length < 6) message = "أدخل كلمة مرور من 6 أحرف على الأقل." else {
                            val backupJson = backupJson(clients, campaigns, templates, logs, followUps.size)
                            shareFile(context, writeSharedFile(context, "waleed_crm_backup_${stamp()}.wcrm", encryptText(backupJson, password)), "application/octet-stream")
                            message = "تم إنشاء نسخة احتياطية مشفرة. احتفظ بكلمة المرور لاستعادتها لاحقاً."
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp)); Text("إنشاء ومشاركة نسخة احتياطية") }
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("استعادة نسخة احتياطية .wcrm", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("يمكن دمج البيانات مع الحالية أو استبدال البيانات الحالية بعد المعاينة.", fontSize = 13.sp)
                    OutlinedTextField(restorePassword, { restorePassword = it }, label = { Text("كلمة مرور الاستعادة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { restoreLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Restore, null); Spacer(Modifier.width(8.dp)); Text("اختيار ملف نسخة احتياطية") }
                    if (pendingRestoreClients.isNotEmpty()) {
                        Text("جاهز للاستعادة: ${pendingRestoreClients.size} سجل", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { viewModel.restoreClientsFromBackup(pendingRestoreClients, false) { i, s -> message = "تم الدمج: $i سجل جديد، وتجاوز $s سجل مكرر."; pendingRestoreClients = emptyList() } }, modifier = Modifier.weight(1f)) { Text("دمج") }
                            Button(onClick = { viewModel.restoreClientsFromBackup(pendingRestoreClients, true) { i, _ -> message = "تم استبدال البيانات واستعادة $i سجل."; pendingRestoreClients = emptyList() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("استبدال") }
                        }
                    }
                }
            }

            ExportButton("استيراد عملاء/أطباء من CSV", "الأعمدة: name, phone, type, specialization, class, location, notes", Icons.Default.FileUpload) { importCsvLauncher.launch("text/*") }

            if (message.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(14.dp)) { Text(message, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun InfoCard() { Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp)) { Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(6.dp)); Text("إدارة البيانات والنسخ الاحتياطي", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("تصدير، استيراد CSV، ونسخ احتياطي مشفر قابل للاستعادة.", fontSize = 13.sp) } } }

@Composable
private fun ExportButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { Card(shape = RoundedCornerShape(16.dp)) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), shape = RoundedCornerShape(16.dp)) { Icon(icon, null); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 12.sp) } } } }

private fun clientsToCsv(clients: List<Client>): String = buildString { appendLine("id,name,phone,second_phone,type,specialization,class,location,notes,date_added,updated_at"); clients.forEach { c -> appendLine(listOf(c.id, c.name, c.phone, c.secondPhone, c.clientType, c.specialization, c.clientClass, c.location, c.notes, c.dateAdded, c.updatedAt).joinToString(",") { csvEscape(it.toString()) }) } }
private fun logsToCsv(logs: List<MessageLog>): String = buildString { appendLine("id,client_id,timestamp,message_text,attachment_name,attachment_type,send_mode,campaign_id,status"); logs.forEach { l -> appendLine(listOf(l.id, l.clientId, l.timestamp, l.messageText, l.attachmentName, l.attachmentType, l.sendMode, l.campaignId, l.status).joinToString(",") { csvEscape(it.toString()) }) } }
private fun campaignsToCsv(campaigns: List<MessageCampaign>): String = buildString { appendLine("id,title,target_count,sent_count,message_mode,attachment_name,date_created"); campaigns.forEach { c -> appendLine(listOf(c.id, c.title, c.targetCount, c.sentCount, c.messageMode, c.attachmentName, c.dateCreated).joinToString(",") { csvEscape(it.toString()) }) } }

private fun backupJson(clients: List<Client>, campaigns: List<MessageCampaign>, templates: List<com.waleed.crm.data.MessageTemplate>, logs: List<MessageLog>, followUpsCount: Int): String = buildString {
    append("{\"version\":2,\"createdAt\":${System.currentTimeMillis()},")
    append("\"clients\":[${clients.joinToString(",") { clientJson(it) }}],")
    append("\"campaignsCount\":${campaigns.size},\"templatesCount\":${templates.size},\"logsCount\":${logs.size},\"followUpsCount\":$followUpsCount}")
}
private fun clientJson(c: Client): String = "{\"name\":\"${jsonEscape(c.name)}\",\"phone\":\"${jsonEscape(c.phone)}\",\"secondPhone\":\"${jsonEscape(c.secondPhone)}\",\"type\":\"${jsonEscape(c.clientType)}\",\"specialization\":\"${jsonEscape(c.specialization)}\",\"class\":\"${jsonEscape(c.clientClass)}\",\"location\":\"${jsonEscape(c.location)}\",\"notes\":\"${jsonEscape(c.notes)}\"}"
private fun parseClientsFromBackup(json: String): List<Client> { val arr = JSONObject(json).optJSONArray("clients") ?: return emptyList(); return (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); Client(name = o.optString("name"), phone = o.optString("phone"), secondPhone = o.optString("secondPhone"), clientType = o.optString("type", "طبيب"), specialization = o.optString("specialization"), clientClass = o.optString("class", "B"), location = o.optString("location"), notes = o.optString("notes"), isClassified = true) }.filter { it.name.isNotBlank() && it.phone.isNotBlank() } }

private fun encryptText(text: String, password: String): String { val salt = Random.nextBytes(16); val iv = Random.nextBytes(16); val key = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, 120_000, 256)).encoded, "AES"); val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv)); return "WCRM1." + Base64.getEncoder().encodeToString(salt) + "." + Base64.getEncoder().encodeToString(iv) + "." + Base64.getEncoder().encodeToString(cipher.doFinal(text.toByteArray(Charsets.UTF_8))) }
private fun decryptText(text: String, password: String): String { val parts = text.trim().split("."); require(parts.size == 4 && parts[0] == "WCRM1"); val salt = Base64.getDecoder().decode(parts[1]); val iv = Base64.getDecoder().decode(parts[2]); val data = Base64.getDecoder().decode(parts[3]); val key = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, 120_000, 256)).encoded, "AES"); val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv)); return cipher.doFinal(data).toString(Charsets.UTF_8) }
private fun writeSharedFile(context: Context, name: String, content: String): File { val dir = File(context.cacheDir, "shared").apply { mkdirs() }; return File(dir, name).apply { writeText(content, Charsets.UTF_8) } }
private fun shareFile(context: Context, file: File, mimeType: String) { val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file); context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "مشاركة الملف")) }
private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
private fun csvEscape(value: String): String = "\"" + value.replace("\"", "\"\"").replace("\n", " ") + "\""
private fun jsonEscape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
