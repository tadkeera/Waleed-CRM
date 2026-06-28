package com.waleed.crm.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.waleed.crm.data.Client
import com.waleed.crm.ui.viewmodel.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DoctorsScreen(viewModel: CrmViewModel, navController: NavController) {
    val clients by viewModel.clients.collectAsState()
    val specializations by viewModel.specializations.collectAsState()
    val locations by viewModel.locations.collectAsState()

    var selectedSpecialization by remember { mutableStateOf("") }
    var specSearchText by remember { mutableStateOf("") }
    var isSpecDropdownExpanded by remember { mutableStateOf(false) }

    var selectedLocation by remember { mutableStateOf("") }
    var locSearchText by remember { mutableStateOf("") }
    var isLocDropdownExpanded by remember { mutableStateOf(false) }

    var doctorNameSearchText by remember { mutableStateOf("") }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedDoctorIds = remember { mutableStateListOf<Long>() }

    val doctors = clients.filter { it.clientType == "طبيب" && it.isClassified }
    val filteredDoctors = doctors.filter { doctor ->
        (selectedSpecialization.isBlank() || doctor.specialization == selectedSpecialization) &&
        (selectedLocation.isBlank() || doctor.location == selectedLocation) &&
        (doctorNameSearchText.length < 3 || doctor.name.contains(doctorNameSearchText, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMultiSelectMode) "تم تحديد (${selectedDoctorIds.size})" else "الأطباء المصنفين", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            val selectableDoctors = if (doctorNameSearchText.length in 1..2) emptyList() else filteredDoctors
                            if (selectedDoctorIds.size == selectableDoctors.size) {
                                selectedDoctorIds.clear()
                            } else {
                                selectedDoctorIds.clear()
                                selectedDoctorIds.addAll(selectableDoctors.map { it.id })
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "تحديد الكل")
                        }
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedDoctorIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = doctorNameSearchText,
                onValueChange = { doctorNameSearchText = it },
                label = { Text("بحث باسم الطبيب (اكتب 3 أحرف)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (doctorNameSearchText.isNotBlank()) {
                        IconButton(onClick = { doctorNameSearchText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            val nameSearchHint = doctorNameSearchText.length in 1..2
            if (nameSearchHint) {
                Text(
                    text = "اكتب 3 أحرف على الأقل لعرض بطاقات الأطباء المشابهة.",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp, end = 4.dp)
                )
            }

            // Revamped Specialization Search Filter Box
            ExposedDropdownMenuBox(
                expanded = isSpecDropdownExpanded,
                onExpandedChange = { isSpecDropdownExpanded = !isSpecDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = specSearchText,
                    onValueChange = {
                        specSearchText = it
                        isSpecDropdownExpanded = it.length >= 3
                        if (it.isBlank()) selectedSpecialization = ""
                    },
                    label = { Text("بحث باسم التخصص (اكتب 3 أحرف)") },
                    trailingIcon = {
                        if (specSearchText.isNotBlank()) {
                            IconButton(onClick = {
                                specSearchText = ""
                                selectedSpecialization = ""
                                isSpecDropdownExpanded = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح الفلتر")
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSpecDropdownExpanded)
                        }
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                val matchingSpecs = specializations.filter { it.name.contains(specSearchText, ignoreCase = true) }
                if (matchingSpecs.isNotEmpty() && specSearchText.length >= 3) {
                    ExposedDropdownMenu(
                        expanded = isSpecDropdownExpanded,
                        onDismissRequest = { isSpecDropdownExpanded = false }
                    ) {
                        matchingSpecs.forEach { spec ->
                            DropdownMenuItem(
                                text = { Text(spec.name) },
                                onClick = {
                                    selectedSpecialization = spec.name
                                    specSearchText = spec.name
                                    isSpecDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Revamped Location Search Filter Box
            ExposedDropdownMenuBox(
                expanded = isLocDropdownExpanded,
                onExpandedChange = { isLocDropdownExpanded = !isLocDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = locSearchText,
                    onValueChange = {
                        locSearchText = it
                        isLocDropdownExpanded = it.length >= 3
                        if (it.isBlank()) selectedLocation = ""
                    },
                    label = { Text("بحث باسم موقع العمل (اكتب 3 أحرف)") },
                    trailingIcon = {
                        if (locSearchText.isNotBlank()) {
                            IconButton(onClick = {
                                locSearchText = ""
                                selectedLocation = ""
                                isLocDropdownExpanded = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح الفلتر")
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLocDropdownExpanded)
                        }
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                val matchingLocs = locations.filter { it.name.contains(locSearchText, ignoreCase = true) }
                if (matchingLocs.isNotEmpty() && locSearchText.length >= 3) {
                    ExposedDropdownMenu(
                        expanded = isLocDropdownExpanded,
                        onDismissRequest = { isLocDropdownExpanded = false }
                    ) {
                        matchingLocs.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc.name) },
                                onClick = {
                                    selectedLocation = loc.name
                                    locSearchText = loc.name
                                    isLocDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Bulk Send Button
            if (isMultiSelectMode && selectedDoctorIds.isNotEmpty()) {
                Button(
                    onClick = {
                        viewModel.selectedDoctorIdsForBulk = selectedDoctorIds.toMutableList()
                        navController.navigate("bulk_message?specialization=$selectedSpecialization&location=$selectedLocation")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Message, contentDescription = "مراسلة جماعية")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال رسالة جماعية للمحددين (${selectedDoctorIds.size} طبيب)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else if (selectedSpecialization.isNotBlank() || selectedLocation.isNotBlank()) {
                Button(
                    onClick = {
                        viewModel.selectedDoctorIdsForBulk = filteredDoctors.map { it.id }.toMutableList()
                        navController.navigate("bulk_message?specialization=$selectedSpecialization&location=$selectedLocation")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Message, contentDescription = "مراسلة جماعية")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال رسالة جماعية في واتساب (${filteredDoctors.size} طبيب)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Message, contentDescription = "مراسلة جماعية")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انقر مطولاً على أي بطاقة طبيب لتحديد الأطباء للمراسلة", fontSize = 14.sp)
                }
            }

            val doctorsToShow = if (doctorNameSearchText.length in 1..2) emptyList() else filteredDoctors

            if (doctorsToShow.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (doctorNameSearchText.length in 1..2) "ابدأ بكتابة 3 أحرف من اسم الطبيب لعرض النتائج."
                        else "لا يوجد أطباء مطابقين للفلاتر المحددة.",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(doctorsToShow, key = { it.id }) { doctor ->
                        val isSelected = selectedDoctorIds.contains(doctor.id)
                        
                        DoctorCardSelectable(
                            client = doctor,
                            onClick = {
                                if (isMultiSelectMode) {
                                    if (isSelected) selectedDoctorIds.remove(doctor.id) else selectedDoctorIds.add(doctor.id)
                                    if (selectedDoctorIds.isEmpty()) isMultiSelectMode = false
                                }
                                // Single click navigation disabled as requested
                            },
                            onLongClick = {
                                isMultiSelectMode = true
                                if (!isSelected) selectedDoctorIds.add(doctor.id)
                            },
                            onEdit = { navController.navigate("add_edit_client/${doctor.id}?phone=") },
                            onDelete = { viewModel.deleteClient(doctor.id) },
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) selectedDoctorIds.remove(doctor.id) else selectedDoctorIds.add(doctor.id)
                                if (selectedDoctorIds.isEmpty()) isMultiSelectMode = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DoctorCardSelectable(
    client: Client,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    val cardBgColor = try {
        Color(android.graphics.Color.parseColor(client.cardColor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { onLongClick() }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = client.clientClass,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = client.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = client.phone, color = Color.Gray, fontSize = 14.sp)
                if (client.secondPhone.isNotBlank()) {
                    Text(text = "رقم إضافي: ${client.secondPhone}", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(client.clientType, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                if (client.clientType == "طبيب" && client.specialization.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = client.specialization, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                if (client.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = client.location, color = Color.DarkGray, fontSize = 12.sp)
                }
            }
        }
    }
}
