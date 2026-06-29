package com.waleed.crm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.waleed.crm.ui.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchitecturePerformanceScreen(viewModel: CrmViewModel, navController: NavController) {
    var items by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { viewModel.performanceSummary { items = it } }
    Scaffold(topBar = { TopAppBar(title = { Text("الأداء والبنية", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp)) { Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary); Text("تحسين الأداء العميق", fontWeight = FontWeight.Bold, fontSize = 22.sp); Text("تهيئة عملية للتحول التدريجي إلى Room/DAO مع فهارس وسجلات منظمة.") } }
            items.forEach { Card(shape = RoundedCornerShape(14.dp)) { Text(it, Modifier.padding(14.dp), fontWeight = FontWeight.Medium) } }
        }
    }
}
