package com.waleed.crm.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(if (savedPin(context).isBlank()) "قفل التطبيق غير مفعل" else "قفل التطبيق مفعل") }
    Scaffold(topBar = { TopAppBar(title = { Text("الأمان والخصوصية", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp)) { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary); Text("حماية التطبيق", fontWeight = FontWeight.Bold, fontSize = 22.sp); Text("إضافة رمز PIN محلي لحماية بيانات الأطباء والعملاء.") } }
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("رمز PIN جديد 4-8 أرقام") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Button(onClick = { if (pin.length >= 4) { savePin(context, pin); pin = ""; message = "تم تفعيل/تحديث قفل التطبيق." } else message = "أدخل 4 أرقام على الأقل." }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("تفعيل أو تحديث القفل") }
            OutlinedButton(onClick = { clearPin(context); message = "تم إلغاء قفل التطبيق." }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("إلغاء القفل") }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(14.dp)) { Text(message, Modifier.padding(14.dp), fontWeight = FontWeight.Bold) }
            Text("ملاحظة: الرمز محفوظ محلياً داخل التطبيق ولا يتم إرساله لأي خادم.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
