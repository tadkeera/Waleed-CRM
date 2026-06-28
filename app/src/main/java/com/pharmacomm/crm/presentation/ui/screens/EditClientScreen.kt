package com.pharmacomm.crm.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.ClientType
import com.pharmacomm.crm.domain.model.ImportanceClass
import com.pharmacomm.crm.domain.model.PhoneNumber
import com.pharmacomm.crm.presentation.viewmodel.EditClientViewModel
import com.pharmacomm.crm.utils.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClientScreen(
    clientId: Long,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: EditClientViewModel = viewModel {
        EditClientViewModel(
            clientRepository = AppContainer.clientRepository,
            lookupRepository = AppContainer.lookupRepository
        )
    }

    val client by viewModel.client.collectAsState()
    val phoneNumbers by viewModel.phoneNumbers.collectAsState()
    val specialties by viewModel.specialties.collectAsState(initial = emptyList())
    val regions by viewModel.regions.collectAsState(initial = emptyList())

    var name by remember { mutableStateOf("") }
    var clientType by remember { mutableStateOf(ClientType.DOCTOR) }
    var importanceClass by remember { mutableStateOf(ImportanceClass.CLASS_B) }
    var specialty by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    val phoneList = remember { mutableStateListOf<String>() }

    // Load existing client if editing
    LaunchedEffect(clientId) {
        if (clientId > 0) {
            viewModel.loadClient(clientId)
        }
    }

    LaunchedEffect(client) {
        client?.let {
            name = it.name
            clientType = it.clientType
            importanceClass = it.importanceClass
            specialty = it.specialty ?: ""
            region = it.region ?: ""
            notes = it.notes ?: ""
        }
    }

    LaunchedEffect(phoneNumbers) {
        phoneList.clear()
        phoneList.addAll(phoneNumbers.map { it.number })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (clientId > 0) "تعديل العميل" else "إضافة عميل جديد") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل") },
                modifier = Modifier.fillMaxWidth()
            )

            // Client Type Dropdown
            ClientTypeDropdown(
                selected = clientType,
                onSelected = { clientType = it }
            )

            // Importance Class
            ImportanceClassDropdown(
                selected = importanceClass,
                onSelected = { importanceClass = it }
            )

            // Specialty (Autocomplete)
            AutocompleteField(
                label = "التخصص",
                value = specialty,
                suggestions = specialties.map { it.name },
                onValueChange = { specialty = it },
                onAddNew = { viewModel.addSpecialty(it) }
            )

            // Region
            AutocompleteField(
                label = "المنطقة / الموقع",
                value = region,
                suggestions = regions.map { it.name },
                onValueChange = { region = it },
                onAddNew = { viewModel.addRegion(it) }
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات إضافية") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Phone Numbers Section
            Text("أرقام الهاتف", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newPhone.isNotBlank()) {
                            phoneList.add(newPhone)
                            newPhone = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة رقم")
                }
            }

            // List of phones
            phoneList.forEachIndexed { index, phone ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(phone, modifier = Modifier.weight(1f))
                    TextButton(onClick = { phoneList.removeAt(index) }) {
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val newClient = Client(
                        id = clientId,
                        name = name.ifBlank { "عميل جديد" },
                        clientType = clientType,
                        importanceClass = importanceClass,
                        specialty = specialty.ifBlank { null },
                        region = region.ifBlank { null },
                        notes = notes.ifBlank { null }
                    )

                    viewModel.saveClient(
                        client = newClient,
                        phones = phoneList.toList(),
                        onComplete = onSave
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("حفظ العميل")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientTypeDropdown(
    selected: ClientType,
    onSelected: (ClientType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val types = ClientType.values()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("نوع العميل") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportanceClassDropdown(
    selected: ImportanceClass,
    onSelected: (ImportanceClass) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val classes = ImportanceClass.values()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("الكلاس / الأهمية") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            classes.forEach { imp ->
                DropdownMenuItem(
                    text = { Text(imp.displayName) },
                    onClick = {
                        onSelected(imp)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AutocompleteField(
    label: String,
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    onAddNew: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        text = value
    }

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
                expanded = it.length >= 2 && suggestions.isNotEmpty()
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )

        if (expanded) {
            val filtered = suggestions.filter { it.contains(text, ignoreCase = true) }.take(5)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    filtered.forEach { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable {
                                    text = suggestion
                                    onValueChange(suggestion)
                                    expanded = false
                                }
                        )
                    }
                    if (text.isNotBlank() && filtered.isEmpty()) {
                        TextButton(onClick = {
                            onAddNew(text)
                            expanded = false
                        }) {
                            Text("إضافة \"$text\" كجديد")
                        }
                    }
                }
            }
        }
    }
}