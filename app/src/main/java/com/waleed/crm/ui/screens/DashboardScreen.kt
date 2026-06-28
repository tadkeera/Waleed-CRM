package com.waleed.crm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.waleed.crm.ui.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: CrmViewModel, navController: NavController) {
    val analytics by viewModel.dashboardAnalytics.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("داشبورد تحليلي", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (analytics == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val data = analytics!!

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Total Completed Doctors Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "اكتمل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("الأطباء المصنفين ومكتملي البيانات", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${data.totalClassifiedDoctors} طبيب", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                // Section: Doctors needing classification or data completion
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "تنبيه", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إشعار: أطباء بحاجة لتصنيف أو إكمال بيانات (${data.unclassifiedDoctors.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }

                            if (data.unclassifiedDoctors.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("جميع الأطباء مصنفين وبياناتهم مكتملة.", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                data.unclassifiedDoctors.forEach { doctor ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { navController.navigate("add_edit_client/${doctor.id}?phone=") },
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "• ${doctor.name.ifBlank { doctor.phone }}", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text(text = "اكمل البيانات >", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Doctors contacted this week + message count
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = "معلومات", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إشعار: أطباء تم التواصل معهم هذا الأسبوع (${data.contactedDoctorsThisWeek.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            if (data.contactedDoctorsThisWeek.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لم يتم مراسلة أي طبيب خلال هذا الأسبوع حتى الآن.", color = Color.Gray, fontSize = 14.sp)
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                data.contactedDoctorsThisWeek.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "• ${item.client.name}", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(text = "${item.messageCount} رسائل", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Doctors NOT contacted this week
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "تنبيه", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إشعار: أطباء لم يتم مراسلتهم هذا الأسبوع (${data.uncontactedDoctorsThisWeek.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }

                            if (data.uncontactedDoctorsThisWeek.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("ممتاز! تم مراسلة جميع الأطباء هذا الأسبوع.", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 14.sp)
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                data.uncontactedDoctorsThisWeek.forEachIndexed { index, doctor ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(34.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = doctor.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                if (doctor.specialization.isNotBlank() || doctor.location.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = listOf(doctor.specialization, doctor.location).filter { it.isNotBlank() }.joinToString(" • "),
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.68f)
                                                    )
                                                }
                                            }
                                            Badge(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)) {
                                                Text(
                                                    text = "لم يراسل",
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
