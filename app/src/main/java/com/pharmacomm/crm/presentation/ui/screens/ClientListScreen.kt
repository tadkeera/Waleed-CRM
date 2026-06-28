package com.pharmacomm.crm.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.presentation.viewmodel.ClientListViewModel
import com.pharmacomm.crm.presentation.ui.components.ClientCard
import com.pharmacomm.crm.utils.ContactImporter

@Composable
fun ClientListScreen(
    viewModel: ClientListViewModel,
    onClientClick: (Long) -> Unit,
    onAddClient: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Top search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("ابحث عن العملاء...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddClient,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("إضافة عميل")
            }

            Button(
                onClick = {
                    // Import contacts
                    ContactImporter.importContacts(context) { contacts ->
                        viewModel.importContacts(contacts)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ImportContacts, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("استيراد جهات")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.clients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("لا يوجد عملاء بعد", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("أضف عميلاً أو استورد جهات الاتصال", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.clients, key = { it.id }) { client ->
                    ClientCard(
                        client = client,
                        onClick = { onClientClick(client.id) },
                        onDelete = { viewModel.deleteClient(client) }
                    )
                }
            }
        }
    }
}