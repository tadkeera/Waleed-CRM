package com.waleed.crm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.waleed.crm.data.DashboardAnalytics
import com.waleed.crm.data.DoctorMessageCount
import com.waleed.crm.data.MessageCampaign
import com.waleed.crm.data.StatItem
import com.waleed.crm.ui.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: CrmViewModel, navController: NavController) {
    val analytics by viewModel.dashboardAnalytics.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadInitialData() }
    Scaffold(topBar = { TopAppBar(title = { Text("داشبورد وتحليلات متقدمة", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ExecutiveSummary(analytics) }
            item { CampaignPerformanceCard(analytics) }
            item { SectionHeader("مؤشرات الأداء", Icons.Default.Analytics) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { MetricCard("رسائل آخر 7 أيام", analytics.weeklyMessages.toString(), Icons.Default.Message, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f)); MetricCard("رسائل آخر 30 يوم", analytics.monthlyMessages.toString(), Icons.Default.Schedule, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f)) } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { MetricCard("إجمالي الحملات", analytics.totalCampaigns.toString(), Icons.Default.Campaign, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f)); MetricCard("فتح واتساب", "${analytics.totalCampaignOpened}/${analytics.totalCampaignTargets}", Icons.Default.CheckCircle, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f)) } }
            item { TopDoctorsCard(analytics.topContactedDoctors, navController) }
            item { OverdueDoctorsCard(analytics, navController) }
            item { GroupStatsCard("تحليل حسب التخصص", analytics.specializationStats, Icons.Default.MedicalServices) }
            item { GroupStatsCard("تحليل حسب المنطقة", analytics.locationStats, Icons.Default.LocationOn) }
            item { RecentCampaignsCard(analytics.recentCampaigns) }
            item { LegacyAlertsCard(analytics, navController) }
        }
    }
}

@Composable private fun ExecutiveSummary(data: DashboardAnalytics) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)); Spacer(Modifier.width(12.dp)); Column { Text("ملخص CRM التنفيذي", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("نظرة سريعة على العملاء والأطباء والحملات", fontSize = 13.sp) } }; Spacer(Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { MiniStat("العملاء", data.totalClients.toString(), Modifier.weight(1f)); MiniStat("الأطباء", data.totalDoctors.toString(), Modifier.weight(1f)); MiniStat("المكتمل", data.totalClassifiedDoctors.toString(), Modifier.weight(1f)) } } } }
@Composable private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) { Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp); Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) } } }
@Composable private fun MetricCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) { Card(modifier = modifier.height(112.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = color)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary); Column { Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp); Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) } } } }
@Composable private fun CampaignPerformanceCard(data: DashboardAnalytics) { val progress = if (data.totalCampaignTargets == 0) 0f else data.totalCampaignOpened.toFloat() / data.totalCampaignTargets.toFloat(); Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("أداء حملات واتساب", fontWeight = FontWeight.Bold, fontSize = 18.sp) }; Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp)); Spacer(Modifier.height(8.dp)); Text("تم فتح واتساب لـ ${data.totalCampaignOpened} من أصل ${data.totalCampaignTargets} مستهدف", fontSize = 13.sp, color = Color.Gray); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { AssistChip(onClick = {}, label = { Text("نص: ${data.textOnlyCampaigns}") }); AssistChip(onClick = {}, label = { Text("مرفق: ${data.attachmentOnlyCampaigns}") }); AssistChip(onClick = {}, label = { Text("نص+مرفق: ${data.textAndAttachmentCampaigns}") }) } } } }
@Composable private fun SectionHeader(title: String, icon: ImageVector) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }
@Composable private fun TopDoctorsCard(items: List<DoctorMessageCount>, navController: NavController) { AnalysisListCard("أكثر الأطباء تواصلاً", Icons.Default.Star) { if (items.isEmpty()) EmptyText("لا يوجد سجل تواصل كافٍ حتى الآن.") else items.forEachIndexed { i, item -> ListRow("${i + 1}", item.client.name, listOf(item.client.specialization, item.client.location).filter { it.isNotBlank() }.joinToString(" • "), "${item.messageCount} رسائل") { navController.navigate("client_details/${item.client.id}") } } } }
@Composable private fun OverdueDoctorsCard(data: DashboardAnalytics, navController: NavController) { AnalysisListCard("أطباء لم يتم التواصل معهم منذ 30 يوم", Icons.Default.Warning) { if (data.overdueDoctors.isEmpty()) EmptyText("لا توجد أسماء متأخرة حسب السجل الحالي.") else data.overdueDoctors.forEachIndexed { i, doctor -> ListRow("${i + 1}", doctor.name, listOf(doctor.specialization, doctor.location).filter { it.isNotBlank() }.joinToString(" • "), "متأخر") { navController.navigate("client_details/${doctor.id}") } } } }
@Composable private fun GroupStatsCard(title: String, stats: List<StatItem>, icon: ImageVector) { AnalysisListCard(title, icon) { if (stats.isEmpty()) EmptyText("لا توجد بيانات كافية للتحليل.") else { val max = stats.maxOfOrNull { it.count } ?: 1; stats.forEach { stat -> Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stat.label, fontWeight = FontWeight.Medium); Text("${stat.count}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(4.dp)); LinearProgressIndicator(progress = { stat.count.toFloat() / max.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp)) } } } } }
@Composable private fun RecentCampaignsCard(campaigns: List<MessageCampaign>) { AnalysisListCard("آخر حملات واتساب", Icons.Default.Campaign) { if (campaigns.isEmpty()) EmptyText("لا توجد حملات محفوظة حتى الآن.") else campaigns.forEach { c -> val label = when (c.messageMode) { "TEXT_ONLY" -> "نص فقط"; "ATTACHMENT_ONLY" -> "مرفق فقط"; "TEXT_AND_ATTACHMENT" -> "نص مع مرفق"; else -> c.messageMode }; ListRow("#${c.id}", c.title, "${formatDate(c.dateCreated)} • $label", "${c.sentCount}/${c.targetCount}") {} } } }
@Composable private fun AnalysisListCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp) }; Spacer(Modifier.height(10.dp)); content() } } }
@Composable private fun ListRow(leading: String, title: String, subtitle: String, badge: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(leading, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp); if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = Color.Gray) }; Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) { Text(badge, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } } }
@Composable private fun LegacyAlertsCard(data: DashboardAnalytics, navController: NavController) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp)); Text("تنبيهات مهمة", fontWeight = FontWeight.Bold, fontSize = 17.sp) }; Spacer(Modifier.height(8.dp)); Text("أطباء بحاجة لإكمال البيانات: ${data.unclassifiedDoctors.size}"); Text("أطباء لم تتم مراسلتهم هذا الأسبوع: ${data.uncontactedDoctorsThisWeek.size}"); data.unclassifiedDoctors.take(5).forEach { d -> Text("• ${d.name}", modifier = Modifier.clickable { navController.navigate("add_edit_client/${d.id}?phone=") }.padding(vertical = 3.dp)) } } } }
@Composable private fun EmptyText(text: String) { Text(text, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(8.dp)) }
private fun formatDate(time: Long): String = try { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(time)) } catch (_: Exception) { "-" }
