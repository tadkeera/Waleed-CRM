package com.waleed.crm.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import com.waleed.crm.data.FollowUpWithClient
import com.waleed.crm.ui.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpsScreen(viewModel: CrmViewModel, navController: NavController) {
    val followUps by viewModel.followUps.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshFollowUps() }
    Scaffold(
        topBar = { TopAppBar(title = { Text("المتابعات والتذكيرات", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("قائمة المتابعات القادمة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("اضغط على اسم الطبيب لفتح التفاصيل، أو ضع علامة إنجاز عند إتمام المتابعة.", fontSize = 13.sp)
                }
            }
            if (followUps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد متابعات معلقة حالياً.", color = MaterialTheme.colorScheme.outline, fontSize = 16.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(followUps, key = { it.followUp.id }) { item ->
                        FollowUpCard(item, viewModel, navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUpCard(item: FollowUpWithClient, viewModel: CrmViewModel, navController: NavController) {
    val context = LocalContext.current
    val client = item.client
    val overdue = item.followUp.dueAt < System.currentTimeMillis()
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.followUp.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(client?.name ?: "عميل غير معروف", fontWeight = FontWeight.Medium)
                    Text("الموعد: ${formatFollowDate(item.followUp.dueAt)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    if (item.followUp.notes.isNotBlank()) Text(item.followUp.notes, fontSize = 13.sp)
                }
                AssistChip(onClick = {}, label = { Text(if (overdue) "متأخر" else "قادم") })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { if (client != null) navController.navigate("client_details/${client.id}") }, modifier = Modifier.weight(1f)) { Text("تفاصيل") }
                OutlinedButton(onClick = {
                    if (client != null) {
                        val clean = client.phone.replace("+", "").replace(" ", "").replace("-", "")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$clean")))
                    }
                }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Message, null); Spacer(Modifier.width(4.dp)); Text("واتساب") }
                IconButton(onClick = { viewModel.completeFollowUp(item.followUp.id) }) { Icon(Icons.Default.CheckCircle, contentDescription = "تم", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { viewModel.deleteFollowUp(item.followUp.id) }) { Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun formatFollowDate(time: Long): String = try {
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(time))
} catch (_: Exception) { "-" }
