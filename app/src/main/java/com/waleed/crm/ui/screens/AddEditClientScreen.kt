package com.waleed.crm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.waleed.crm.data.Client
import com.waleed.crm.data.withDoctorPrefix
import com.waleed.crm.data.withYemenPhoneCode
import com.waleed.crm.ui.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClientScreen(
    viewModel: CrmViewModel,
    navController: NavController,
    clientId: Long,
    incomingPhone: String
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(if (incomingPhone.isBlank()) "+967" else incomingPhone.withYemenPhoneCode()) }
    var secondPhone by remember { mutableStateOf("+967") }
    var showSecondPhone by remember { mutableStateOf(false) }
    var clientType by remember { mutableStateOf("طبيب") } // طبيب، صيدلي، مدير مشتريات
    var specialization by remember { mutableStateOf("") }
    var clientClass by remember { mutableStateOf("B") } // A, B, C
    var location by remember { mutableStateOf("") }
    var clientCardColor by remember { mutableStateOf("#2196F3") }

    var currentClientId by remember { mutableStateOf(clientId) }
    var newPharmacyName by remember { mutableStateOf("") }

    val specializations by viewModel.specializations.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val nearbyPharmacies by viewModel.nearbyPharmacies.collectAsState()

    var isSpecDropdownExpanded by remember { mutableStateOf(false) }
    var isLocDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentClientId) {
        if (currentClientId != 0L) {
            viewModel.getClientById(currentClientId) { client ->
                if (client != null) {
                    name = if (client.clientType == "طبيب") client.name.withDoctorPrefix() else client.name
                    phone = client.phone.withYemenPhoneCode()
                    secondPhone = if (client.secondPhone.isBlank()) "+967" else client.secondPhone.withYemenPhoneCode()
                    if (secondPhone.isNotBlank()) showSecondPhone = true
                    clientType = client.clientType
                    specialization = client.specialization
                    clientClass = client.clientClass
                    location = client.location
                    clientCardColor = client.cardColor
                    viewModel.loadNearbyPharmacies(currentClientId)
                }
            }
        } else if (incomingPhone.isNotBlank()) {
            phone = incomingPhone.withYemenPhoneCode()
        } else {
            phone = "+967"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentClientId == 0L) "إضافة عميل جديد" else "تعديل بيانات العميل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (currentClientId != 0L) {
                        IconButton(onClick = {
                            viewModel.deleteClient(currentClientId) {
                                navController.popBackStack()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف العميل", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = if (clientType == "طبيب") it.withDoctorPrefix() else it },
                    label = { Text("الاسم") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.ifBlank { "+967" } },
                        label = { Text("رقم الهاتف الرئيسي") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (!showSecondPhone) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                showSecondPhone = true
                                if (secondPhone.isBlank()) secondPhone = "+967"
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة رقم آخر", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = showSecondPhone) {
                    OutlinedTextField(
                        value = secondPhone,
                        onValueChange = { secondPhone = it.ifBlank { "+967" } },
                        label = { Text("رقم الهاتف الإضافي") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            item {
                Text("نوع العميل", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    val types = listOf("طبيب", "صيدلي", "مدير مشتريات")
                    types.forEach { type ->
                        val selected = clientType == type
                        FilterChip(
                            selected = selected,
                            onClick = { clientType = type },
                            label = { Text(type, modifier = Modifier.padding(horizontal = 8.dp)) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            if (clientType == "طبيب") {
                item {
                    ExposedDropdownMenuBox(
                        expanded = isSpecDropdownExpanded,
                        onExpandedChange = { isSpecDropdownExpanded = !isSpecDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = specialization,
                            onValueChange = {
                                specialization = it
                                isSpecDropdownExpanded = it.length >= 3
                            },
                            label = { Text("التخصص (اكتب أول 3 أحرف للبحث أو أضف جديد)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSpecDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        val matchingSpecs = specializations.filter { it.name.contains(specialization, ignoreCase = true) }
                        if (matchingSpecs.isNotEmpty() && specialization.length >= 3) {
                            ExposedDropdownMenu(
                                expanded = isSpecDropdownExpanded,
                                onDismissRequest = { isSpecDropdownExpanded = false }
                            ) {
                                matchingSpecs.forEach { spec ->
                                    DropdownMenuItem(
                                        text = { Text(spec.name) },
                                        onClick = {
                                            specialization = spec.name
                                            clientCardColor = spec.color
                                            isSpecDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = isLocDropdownExpanded,
                    onExpandedChange = { isLocDropdownExpanded = !isLocDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            isLocDropdownExpanded = it.length >= 3
                        },
                        label = { Text("موقع أو منطقة عمل العميل (اكتب أول 3 أحرف)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLocDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    val matchingLocs = locations.filter { it.name.contains(location, ignoreCase = true) }
                    if (matchingLocs.isNotEmpty() && location.length >= 3) {
                        ExposedDropdownMenu(
                            expanded = isLocDropdownExpanded,
                            onDismissRequest = { isLocDropdownExpanded = false }
                        ) {
                            matchingLocs.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc.name) },
                                    onClick = {
                                        location = loc.name
                                        isLocDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("تصنيف الفئة / الكلاس", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    val classes = listOf("A", "B", "C")
                    classes.forEach { cls ->
                        val selected = clientClass == cls
                        FilterChip(
                            selected = selected,
                            onClick = { clientClass = cls },
                            label = { Text("Class $cls", modifier = Modifier.padding(horizontal = 16.dp)) },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            }

            // Nearby Pharmacies Section (linked to Client ID)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalPharmacy, contentDescription = "الصيدليات القريبة", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الصيدليات القريبة المخصصة للطبيب", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (currentClientId == 0L) {
                            Text("يرجى حفظ بيانات الطبيب أولاً لتتمكن من إضافة الصيدليات القريبة المخصصة له مباشرة.", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (name.isNotBlank() && phone.isNotBlank()) {
                                        val client = Client(
                                            id = currentClientId,
                                            name = if (clientType == "طبيب") name.withDoctorPrefix() else name.trim(),
                                            phone = phone.withYemenPhoneCode(),
                                            secondPhone = if (secondPhone.isBlank() || secondPhone == "+967") "" else secondPhone.withYemenPhoneCode(),
                                            clientType = clientType,
                                            specialization = if (clientType == "طبيب") specialization else "",
                                            clientClass = clientClass,
                                            location = location,
                                            isClassified = true,
                                            cardColor = clientCardColor
                                        )
                                        viewModel.saveClient(client) { newId ->
                                            currentClientId = newId
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حفظ مبدئي لتفعيل إضافة الصيدليات")
                            }
                        } else {
                            if (nearbyPharmacies.isEmpty()) {
                                Text("لا توجد صيدليات قريبة مضافة لهذا الطبيب حتى الآن.", color = Color.Gray, fontSize = 14.sp)
                            } else {
                                nearbyPharmacies.forEach { pharmacy ->
                                    Text(text = "• ${pharmacy.name}", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPharmacyName,
                                    onValueChange = { newPharmacyName = it },
                                    label = { Text("اسم الصيدلية القريبة للطبيب") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newPharmacyName.isNotBlank()) {
                                            viewModel.addPharmacy(newPharmacyName, currentClientId)
                                            newPharmacyName = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "إضافة صيدلية")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            val client = Client(
                                id = currentClientId,
                                name = if (clientType == "طبيب") name.withDoctorPrefix() else name.trim(),
                                phone = phone.withYemenPhoneCode(),
                                secondPhone = if (secondPhone.isBlank() || secondPhone == "+967") "" else secondPhone.withYemenPhoneCode(),
                                clientType = clientType,
                                specialization = if (clientType == "طبيب") specialization else "",
                                clientClass = clientClass,
                                location = location,
                                isClassified = true,
                                cardColor = clientCardColor
                            )
                            viewModel.saveClient(client) {
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "حفظ البيانات")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ بيانات العميل", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
