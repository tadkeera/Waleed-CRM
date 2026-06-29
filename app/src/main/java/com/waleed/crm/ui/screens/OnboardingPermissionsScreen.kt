package com.waleed.crm.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

private const val PREF = "waleed_onboarding"
private const val DONE = "done"

fun isOnboardingDone(context: Context): Boolean = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(DONE, false)
fun setOnboardingDone(context: Context) { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(DONE, true).apply() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("جهّز التطبيق بخطوات واضحة قبل طلب الصلاحيات.") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        message = "تم تحديث الصلاحيات: ${result.count { it.value }} صلاحية مفعلة."
    }
    Scaffold(topBar = { TopAppBar(title = { Text("الإعداد الأولي والصلاحيات", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.Start, null, tint = MaterialTheme.colorScheme.primary)
                    Text("مرحباً بك في Waleed CRM", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("هذه الشاشة توضح الصلاحيات المطلوبة ويمكن الرجوع لها من شريط التنقل.")
                }
            }
            PermissionRow("إشعارات المتابعات", "لاستقبال تذكير موعد المتابعة.", Icons.Default.NotificationsActive)
            PermissionRow("حالة الهاتف", "للتعرف على الأرقام الواردة وإضافة عميل بسرعة.", Icons.Default.PhoneAndroid)
            Button(onClick = {
                val permissions = mutableListOf(Manifest.permission.READ_PHONE_STATE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                launcher.launch(permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }.toTypedArray())
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("طلب الصلاحيات المطلوبة") }
            OutlinedButton(onClick = { setOnboardingDone(context); navController.navigate("contacts_list") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("إنهاء الإعداد الأولي") }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(14.dp)) { Text(message, Modifier.padding(14.dp), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun PermissionRow(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(body, fontSize = 13.sp) } } }
}
