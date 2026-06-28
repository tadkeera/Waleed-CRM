package com.waleed.crm.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.waleed.crm.data.Client
import com.waleed.crm.data.GalleryFile
import com.waleed.crm.ui.viewmodel.CrmViewModel
import coil.compose.AsyncImage
import java.io.File
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkMessageScreen(
    viewModel: CrmViewModel,
    navController: NavController,
    specialization: String,
    location: String
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val galleryFiles by viewModel.galleryFiles.collectAsState()

    val targetDoctors = clients.filter { viewModel.selectedDoctorIdsForBulk.contains(it.id) }

    var messageText by remember { mutableStateOf("مرحباً دكتور {name}،\nنود إعلامكم بآخر التحديثات الطبية والأصناف المتاحة لدينا.") }
    var selectedAttachment by remember { mutableStateOf<GalleryFile?>(null) }
    var showGalleryModal by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) } // 0: الصور, 1: الملفات

    var currentSendIndex by remember { mutableStateOf(0) }
    var isSendingFlowStarted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مراسلة جماعية في واتساب", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المستهدفين: الأطباء المحددين", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (specialization.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("التخصص: $specialization", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (location.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("المنطقة: $location", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("العدد الإجمالي: ${targetDoctors.size} طبيب", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            if (!isSendingFlowStarted) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("نص الرسالة (استخدم {name} لاسم الطبيب الخاص)") },
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 10
                )

                Text(
                    text = "ملاحظة: سيتم استبدال الرمز {name} تلقائياً باسم كل طبيب خاص أثناء إرسال الرسالة.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Attachment Selection Button
                OutlinedButton(
                    onClick = { showGalleryModal = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "إضافة مرفقات")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedAttachment == null) "اختيار مرفق من المعرض (صور / PDF)" else "تغيير المرفق (${selectedAttachment?.name})")
                }

                // Beautiful Integrated Image Viewer Preview Box
                if (selectedAttachment != null && selectedAttachment?.type == "image") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("معاينة الصورة المرفقة للإرسال", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                            AsyncImage(
                                model = File(selectedAttachment!!.filePath),
                                contentDescription = "معاينة الصورة",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (targetDoctors.isNotEmpty() && messageText.isNotBlank()) {
                            viewModel.logBulkMessages(targetDoctors.map { it.id })
                            isSendingFlowStarted = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "إرسال")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بدء الإرسال عبر واتساب", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (currentSendIndex < targetDoctors.size) {
                        val currentDoctor = targetDoctors[currentSendIndex]
                        val customizedMessage = messageText.replace("{name}", currentDoctor.name)

                        Text("جاري المراسلة (${currentSendIndex + 1} من ${targetDoctors.size})", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("الطبيب: ${currentDoctor.name}", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text("الرقم: ${currentDoctor.phone}", color = Color.Gray, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                sendWhatsAppMessage(context, currentDoctor.phone, customizedMessage, selectedAttachment)
                                currentSendIndex++
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "إرسال للطبيب")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فتح واتساب وإرسال لـ ${currentDoctor.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = "اكتمل الإرسال", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("تم إكمال مراسلة جميع الأطباء بنجاح!", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("إنهاء والعودة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // App Gallery Selection Modal Sheet with Coil Previews
        if (showGalleryModal) {
            ModalBottomSheet(
                onDismissRequest = { showGalleryModal = false }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("اختر المرفق من المعرض المخصص للتطبيق", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))

                    TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.padding(bottom = 12.dp)) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("الصور", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Image, contentDescription = "الصور") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("الملفات (PDF)", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = "الملفات") }
                        )
                    }

                    val displayedFiles = galleryFiles.filter {
                        if (selectedTabIndex == 0) it.type == "image" else it.type == "pdf"
                    }

                    if (displayedFiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد ملفات في هذا القسم من المعرض.", color = Color.Gray, fontSize = 16.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(displayedFiles, key = { it.id }) { file ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                        selectedAttachment = file
                                        showGalleryModal = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (file.type == "image") {
                                            AsyncImage(
                                                model = File(file.filePath),
                                                contentDescription = file.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(file.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showGalleryModal = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

private fun sendWhatsAppMessage(context: Context, phone: String, message: String, attachment: GalleryFile?) {
    try {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        val encodedMessage = URLEncoder.encode(message, "UTF-8")

        val intent = if (attachment != null) {
            val file = File(attachment.filePath)
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            Intent(Intent.ACTION_SEND).apply {
                type = if (attachment.type == "image") "image/*" else "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("jid", "$cleanPhone@s.whatsapp.net")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")).apply {
                setPackage("com.whatsapp")
            }
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.startActivity(intent)
    } catch (e: Exception) {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"))
        fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(fallbackIntent)
    }
}
