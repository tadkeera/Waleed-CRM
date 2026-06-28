package com.pharmacomm.crm.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pharmacomm.crm.data.local.AppDatabase
import com.pharmacomm.crm.data.repository.ClientRepositoryImpl
import com.pharmacomm.crm.data.repository.ContactLogRepositoryImpl
import com.pharmacomm.crm.data.repository.LookupRepositoryImpl
import com.pharmacomm.crm.domain.usecase.GetClientsUseCase
import com.pharmacomm.crm.domain.usecase.SearchClientsUseCase
import com.pharmacomm.crm.presentation.ui.screens.ClientListScreen
import com.pharmacomm.crm.presentation.ui.screens.DashboardScreen
import com.pharmacomm.crm.presentation.ui.screens.DoctorsMessagingScreen
import com.pharmacomm.crm.presentation.ui.screens.EditClientScreen
import com.pharmacomm.crm.presentation.viewmodel.ClientListViewModel
import com.pharmacomm.crm.utils.AppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize App Container (simple DI)
        val app = application as com.pharmacomm.crm.PharmaCommApplication
        AppContainer.init(app.database)

        setContent {
            PharmaCommCRMTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun PharmaCommCRMTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF1976D2),
            secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            background = androidx.compose.ui.graphics.Color(0xFFF5F5F5)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PharmaComm CRM") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { 
                        selectedTab = 0
                        navController.navigate("clients") { popUpTo("clients") { inclusive = false } }
                    },
                    icon = { Text("👥") },
                    label = { Text("العملاء") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        navController.navigate("doctors") { popUpTo("doctors") { inclusive = false } }
                    },
                    icon = { Text("💊") },
                    label = { Text("الأطباء") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = false } }
                    },
                    icon = { Text("📊") },
                    label = { Text("لوحة التحكم") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "clients",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("clients") {
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.pharmacomm.crm.PharmaCommApplication
                val clientListViewModel: ClientListViewModel = remember {
                    ClientListViewModel(
                        GetClientsUseCase(AppContainer.clientRepository),
                        SearchClientsUseCase(AppContainer.clientRepository),
                        AppContainer.clientRepository
                    )
                }
                ClientListScreen(
                    viewModel = clientListViewModel,
                    onClientClick = { clientId ->
                        // Navigate to edit client (simplified for now)
                        navController.navigate("edit_client/$clientId")
                    },
                    onAddClient = {
                        navController.navigate("edit_client/0")
                    }
                )
            }
            composable("doctors") {
                DoctorsMessagingScreen()
            }
            composable("dashboard") {
                DashboardScreen()
            }
            composable("edit_client/{clientId}") { backStackEntry ->
                val clientId = backStackEntry.arguments?.getString("clientId")?.toLongOrNull() ?: 0L
                EditClientScreen(
                    clientId = clientId,
                    onBack = { navController.popBackStack() },
                    onSave = { navController.popBackStack() }
                )
            }
        }
    }
}

