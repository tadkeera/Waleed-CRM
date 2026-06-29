package com.waleed.crm.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأمان والخصوصية", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "رجوع") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("سياسة خصوصية مختصرة", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("تطبيق Waleed CRM يخزن بيانات العملاء والأطباء محلياً داخل جهازك ولا يرفعها إلى خوادم خارجية.", fontSize = 14.sp)
                }
            }
            SecurityPoint("النسخ الاحتياطي", "تم تعطيل النسخ الاحتياطي التلقائي لقواعد البيانات وملفات التطبيق لحماية بيانات العملاء.")
            SecurityPoint("الملفات والمرفقات", "يتم نسخ المرفقات إلى مساحة التطبيق الخاصة، ومشاركة الملفات تتم عبر FileProvider بصلاحية مؤقتة فقط.")
            SecurityPoint("الصلاحيات", "تم تقليل الصلاحيات المطلوبة، ويتم طلب جهات الاتصال فقط عند استخدام الاستيراد.")
            SecurityPoint("الشبكة", "تم منع الاتصالات غير المشفرة Cleartext Traffic.")
        }
    }
}

@Composable
private fun SecurityPoint(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
