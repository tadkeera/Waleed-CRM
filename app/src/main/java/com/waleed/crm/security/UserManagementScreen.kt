package com.waleed.crm.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.waleed.crm.data.UserAccount
import com.waleed.crm.ui.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(viewModel: CrmViewModel, navController: NavController) {
    val users by viewModel.users.collectAsState()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("USER") }
    Scaffold(topBar = { TopAppBar(title = { Text("المستخدمون والصلاحيات", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp)) { Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.primary); Text("إدارة مستخدمين محلية", fontWeight = FontWeight.Bold, fontSize = 22.sp); Text("إضافة أدوار: ADMIN / USER / VIEWER لاستخدامها في الصلاحيات والتدقيق.") } }
            OutlinedTextField(name, { name = it }, label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(username, { username = it }, label = { Text("اسم الدخول") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("ADMIN","USER","VIEWER").forEach { r -> FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r) }) } }
            Button(onClick = { viewModel.addUser(name, username, role) { name = ""; username = "" } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("إضافة مستخدم") }
            LazyColumn { items(users, key = { it.id }) { UserCard(it, viewModel) } }
        }
    }
}
@Composable private fun UserCard(user: UserAccount, viewModel: CrmViewModel) { Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text(user.name, fontWeight = FontWeight.Bold); Text("${user.username} - ${user.role} - ${if (user.isActive) "نشط" else "معطل"}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { viewModel.updateUserRole(user.id, "ADMIN", true) }) { Text("Admin") }; OutlinedButton(onClick = { viewModel.updateUserRole(user.id, "VIEWER", true) }) { Text("Viewer") }; OutlinedButton(onClick = { viewModel.updateUserRole(user.id, user.role, !user.isActive) }) { Text(if (user.isActive) "تعطيل" else "تفعيل") } } } } }
