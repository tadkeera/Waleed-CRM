package com.waleed.crm.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.waleed.crm.data.Client
import com.waleed.crm.data.SavedSegment
import com.waleed.crm.ui.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SmartSearchScreen(viewModel: CrmViewModel, navController: NavController) {
    val specs by viewModel.specializations.collectAsState()
    val locs by viewModel.locations.collectAsState()
    val results by viewModel.smartSearchResults.collectAsState()
    val saved by viewModel.savedSegments.collectAsState()
    var q by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("الكل") }
    var spec by remember { mutableStateOf("الكل") }
    var loc by remember { mutableStateOf("الكل") }
    var klass by remember { mutableStateOf("الكل") }
    var pending by remember { mutableStateOf(false) }
    var overdue by remember { mutableStateOf(false) }
    fun segment() = SavedSegment(name = name.ifBlank { "بحث ${q}" }, query = q, clientType = type, specialization = spec, location = loc, clientClass = klass, onlyPendingFollowUp = pending, onlyOverdueFollowUp = overdue)
    LaunchedEffect(Unit) { viewModel.runSmartSearch(SavedSegment(name="كل النتائج")) }
    Scaffold(topBar = { TopAppBar(title = { Text("البحث الذكي والتقسيم", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(q, { q = it; viewModel.runSmartSearch(segment()) }, label = { Text("بحث بالاسم/الهاتف/التخصص/المنطقة/الملاحظات") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.ManageSearch, null) })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("الكل","طبيب","عميل").forEach { FilterChip(type==it, { type=it; viewModel.runSmartSearch(segment()) }, label={Text(it)}) }
                listOf("الكل","A","B","C").forEach { FilterChip(klass==it, { klass=it; viewModel.runSmartSearch(segment()) }, label={Text("Class $it")}) }
                FilterChip(pending, { pending=!pending; viewModel.runSmartSearch(segment()) }, label={Text("متابعة معلقة")})
                FilterChip(overdue, { overdue=!overdue; viewModel.runSmartSearch(segment()) }, label={Text("متأخرة")})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = { spec = "الكل"; viewModel.runSmartSearch(segment()) }, label = { Text("كل التخصصات") })
                specs.take(3).forEach { s -> AssistChip(onClick = { spec = s.name; viewModel.runSmartSearch(segment()) }, label = { Text(s.name.take(10)) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = { loc = "الكل"; viewModel.runSmartSearch(segment()) }, label = { Text("كل المناطق") })
                locs.take(3).forEach { l -> AssistChip(onClick = { loc = l.name; viewModel.runSmartSearch(segment()) }, label = { Text(l.name.take(10)) }) }
            }
            OutlinedTextField(name, { name = it }, label = { Text("اسم القائمة المحفوظة") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { viewModel.saveSegment(segment()) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.BookmarkAdd, null); Spacer(Modifier.width(8.dp)); Text("حفظ البحث الحالي") }
            if (saved.isNotEmpty()) LazyColumn(Modifier.heightIn(max=110.dp)) { items(saved, key={it.id}) { s -> AssistChip(onClick={ q=s.query; type=s.clientType; spec=s.specialization; loc=s.location; klass=s.clientClass; pending=s.onlyPendingFollowUp; overdue=s.onlyOverdueFollowUp; viewModel.runSmartSearch(s) }, label={Text(s.name)}) } }
            Text("النتائج: ${results.size}", fontWeight = FontWeight.Bold)
            LazyColumn { items(results, key = { it.id }) { ClientResultCard(it, navController) } }
        }
    }
}
@Composable private fun ClientResultCard(client: Client, navController: NavController) { val context = LocalContext.current; Card(Modifier.fillMaxWidth().padding(vertical=4.dp), shape=RoundedCornerShape(14.dp)) { Column(Modifier.padding(12.dp)) { Text(client.name, fontWeight=FontWeight.Bold); Text("${client.phone} - ${client.specialization} - ${client.location} - ${client.clientClass}"); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={ navController.navigate("client_details/${client.id}") }) { Text("تفاصيل") }; OutlinedButton(onClick={ val clean=client.phone.replace("+","").replace(" ",""); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$clean"))) }) { Icon(Icons.Default.Message,null); Text("واتساب") } } } } }
