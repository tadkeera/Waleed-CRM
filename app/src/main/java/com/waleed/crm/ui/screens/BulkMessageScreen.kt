package com.waleed.crm.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.waleed.crm.data.GalleryFile
import com.waleed.crm.data.MessageCampaign
import com.waleed.crm.data.MessageLog
import com.waleed.crm.data.MessageTemplate
import com.waleed.crm.ui.viewmodel.CrmViewModel
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TEXT_ONLY = "TEXT_ONLY"
private const val ATTACHMENT_ONLY = "ATTACHMENT_ONLY"
private const val TEXT_AND_ATTACHMENT = "TEXT_AND_ATTACHMENT"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkMessageScreen(viewModel: CrmViewModel, navController: NavController, specialization: String, location: String) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val galleryFiles by viewModel.galleryFiles.collectAsState()
    val templates by viewModel.messageTemplates.collectAsState()
    val campaigns by viewModel.messageCampaigns.collectAsState()
    val targets = clients.filter { viewModel.selectedDoctorIdsForBulk.contains(it.id) }
    var messageText by remember { mutableStateOf("مرحباً دكتور {name}،\nنود إعلامكم بآخر التحديثات الطبية والأصناف المتاحة لدينا.") }
    var templateTitle by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TEXT_ONLY) }
    var attachment by remember { mutableStateOf<GalleryFile?>(null) }
    var showTemplates by remember { mutableStateOf(false) }
    var showAttachments by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var index by remember { mutableStateOf(0) }
    var sent by remember { mutableStateOf(0) }
    var campaignId by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { viewModel.refreshMessagingData() }
    Scaffold(topBar = { TopAppBar(title = { Text("مراسلة واتساب احترافية", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp)) { Text("المستهدفون: ${targets.size} طبيب", fontWeight = FontWeight.Bold, fontSize = 18.sp); if (specialization.isNotBlank()) Text("التخصص: $specialization"); if (location.isNotBlank()) Text("المنطقة: $location"); if (sending) Text("تم فتح واتساب لـ $sent من ${targets.size}") } }
            if (!sending) {
                LazyColumn(Modifier.weight(1f)) {
                    item { Text("طريقة الإرسال", fontWeight = FontWeight.Bold); ModeChip(TEXT_ONLY, "نص فقط", mode) { mode = it }; ModeChip(ATTACHMENT_ONLY, "مرفق فقط", mode) { mode = it }; ModeChip(TEXT_AND_ATTACHMENT, "نص مع مرفق", mode) { mode = it }; Spacer(Modifier.height(8.dp)) }
                    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ showTemplates = true }, Modifier.weight(1f)) { Icon(Icons.Default.TextSnippet, null); Spacer(Modifier.width(4.dp)); Text("القوالب") }; OutlinedButton({ viewModel.addMessageTemplate(templateTitle.ifBlank { "قالب ${templates.size + 1}" }, messageText); templateTitle = "" }, Modifier.weight(1f), enabled = messageText.isNotBlank()) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text("حفظ قالب") } }; OutlinedTextField(templateTitle, { templateTitle = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("عنوان القالب - اختياري") }) }
                    if (mode != ATTACHMENT_ONLY) item { OutlinedTextField(messageText, { messageText = it }, Modifier.fillMaxWidth().heightIn(min = 150.dp).padding(vertical = 10.dp), label = { Text("نص الرسالة - استخدم {name}") }, minLines = 6) }
                    if (mode != TEXT_ONLY) item { OutlinedButton({ showAttachments = true }, Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Default.AttachFile, null); Spacer(Modifier.width(8.dp)); Text(attachment?.name ?: "اختيار مرفق") } }
                    item { Spacer(Modifier.height(12.dp)); Button({ val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()); viewModel.createMessageCampaign(MessageCampaign(title = "حملة واتساب $date", targetCount = targets.size, messageMode = mode, attachmentName = attachment?.name ?: "")) { id -> campaignId = id; sending = true; index = 0; sent = 0 } }, Modifier.fillMaxWidth().height(56.dp), enabled = targets.isNotEmpty() && (mode == ATTACHMENT_ONLY || messageText.isNotBlank()) && (mode == TEXT_ONLY || attachment != null)) { Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text("بدء حملة واتساب") } }
                    if (campaigns.isNotEmpty()) { item { Spacer(Modifier.height(16.dp)); Text("آخر الحملات", fontWeight = FontWeight.Bold) }; items(campaigns.take(5), key = { it.id }) { c -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("${c.title}\n${c.sentCount}/${c.targetCount} - ${modeLabel(c.messageMode)}", Modifier.padding(12.dp)) } } }
                }
            } else {
                if (index < targets.size) { val doctor = targets[index]; val msg = messageText.replace("{name}", doctor.name); Spacer(Modifier.weight(1f)); Text("${index + 1} من ${targets.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text(doctor.name); Text(modeLabel(mode), color = Color.Gray); Button({ sendWhatsApp(context, doctor.phone, msg, attachment, mode); sent++; viewModel.logMessage(MessageLog(clientId = doctor.id, messageText = if (mode == ATTACHMENT_ONLY) "" else msg, attachmentName = attachment?.name ?: "", attachmentType = attachment?.type ?: "", sendMode = mode, campaignId = campaignId, status = "OPENED_WHATSAPP")); viewModel.updateCampaignSentCount(campaignId, sent); index++ }, Modifier.fillMaxWidth().height(56.dp)) { Text("فتح واتساب لهذا الطبيب") }; Spacer(Modifier.weight(1f)) } else { Spacer(Modifier.weight(1f)); Text("تم إكمال حملة واتساب", fontWeight = FontWeight.Bold, fontSize = 22.sp); Button({ navController.popBackStack() }, Modifier.fillMaxWidth().height(56.dp)) { Text("إنهاء والعودة") }; Spacer(Modifier.weight(1f)) }
            }
        }
        if (showTemplates) ModalBottomSheet(onDismissRequest = { showTemplates = false }) { SheetTemplates(templates, { messageText = it.body; showTemplates = false }, { viewModel.deleteMessageTemplate(it.id) }) }
        if (showAttachments) ModalBottomSheet(onDismissRequest = { showAttachments = false }) { SheetAttachments(galleryFiles, { attachment = it; showAttachments = false }) }
    }
}

@Composable private fun ModeChip(value: String, title: String, selected: String, onSelect: (String) -> Unit) { Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onSelect(value) }, colors = CardDefaults.cardColors(containerColor = if (selected == value) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) { Row(Modifier.padding(10.dp)) { RadioButton(selected == value, { onSelect(value) }); Text(title, Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold) } } }
@Composable private fun SheetTemplates(items: List<MessageTemplate>, onSelect: (MessageTemplate) -> Unit, onDelete: (MessageTemplate) -> Unit) { LazyColumn(Modifier.padding(16.dp).heightIn(max = 420.dp)) { item { Text("قوالب الرسائل", fontWeight = FontWeight.Bold, fontSize = 20.sp) }; items(items, key = { it.id }) { t -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(t) }) { Row(Modifier.padding(12.dp)) { Column(Modifier.weight(1f)) { Text(t.title, fontWeight = FontWeight.Bold); Text(t.body, maxLines = 2) }; IconButton({ onDelete(t) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) } } } } } }
@Composable private fun SheetAttachments(items: List<GalleryFile>, onSelect: (GalleryFile) -> Unit) { LazyColumn(Modifier.padding(16.dp).heightIn(max = 420.dp)) { item { Text("اختيار مرفق", fontWeight = FontWeight.Bold, fontSize = 20.sp) }; items(items, key = { it.id }) { f -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(f) }) { Text("${f.name} (${f.type})", Modifier.padding(14.dp), fontWeight = FontWeight.Bold) } } } }
private fun modeLabel(mode: String) = when (mode) { TEXT_ONLY -> "نص فقط"; ATTACHMENT_ONLY -> "مرفق فقط"; TEXT_AND_ATTACHMENT -> "نص مع مرفق"; else -> mode }
private fun sendWhatsApp(context: Context, phone: String, message: String, attachment: GalleryFile?, mode: String) { val clean = phone.replace("+", "").replace(" ", "").replace("-", ""); val encoded = URLEncoder.encode(message, "UTF-8"); val intent = if (mode == TEXT_ONLY || attachment == null) Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$clean&text=$encoded")) else { val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(attachment.filePath)); Intent(Intent.ACTION_SEND).apply { type = if (attachment.type == "image") "image/*" else "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); if (mode == TEXT_AND_ATTACHMENT) putExtra(Intent.EXTRA_TEXT, message); putExtra("jid", "$clean@s.whatsapp.net"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) } }; intent.setPackage("com.whatsapp"); intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION; context.startActivity(intent) }
