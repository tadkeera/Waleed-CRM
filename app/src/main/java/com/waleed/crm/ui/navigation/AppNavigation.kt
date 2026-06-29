package com.waleed.crm.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waleed.crm.ui.screens.*
import com.waleed.crm.ui.viewmodel.CrmViewModel
import com.waleed.crm.security.PrivacyScreen
import com.waleed.crm.security.SecuritySettingsScreen
import com.waleed.crm.security.UserManagementScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Contacts : BottomNavItem("contacts_list", "قائمة الأسماء", Icons.Default.Contacts)
    object Doctors : BottomNavItem("doctors_list", "الأطباء", Icons.Default.MedicalServices)
    object Gallery : BottomNavItem("media_gallery", "المعرض", Icons.Default.Collections)
    object Dashboard : BottomNavItem("analytics_dashboard", "داشبورد تحليلي", Icons.Default.Analytics)
    object ImportExport : BottomNavItem("import_export", "استيراد/تصدير", Icons.Default.ImportExport)
    object FollowUps : BottomNavItem("follow_ups", "المتابعات", Icons.Default.Notifications)
    object Reports : BottomNavItem("reports", "التقارير", Icons.Default.Assessment)
    object Security : BottomNavItem("security_settings", "الأمان", Icons.Default.Security)
    object Onboarding : BottomNavItem("onboarding", "الإعداد", Icons.Default.Start)
    object Sync : BottomNavItem("cloud_sync", "المزامنة", Icons.Default.CloudSync)
    object Users : BottomNavItem("users", "المستخدمون", Icons.Default.Group)
    object Audit : BottomNavItem("audit_logs", "السجل", Icons.Default.History)
    object SmartSearch : BottomNavItem("smart_search", "بحث ذكي", Icons.Default.ManageSearch)
    object Performance : BottomNavItem("performance", "الأداء", Icons.Default.Speed)
}

@Composable
fun AppNavigation(
    viewModel: CrmViewModel,
    initialRoute: String = BottomNavItem.Contacts.route,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val items = listOf(BottomNavItem.Contacts, BottomNavItem.Doctors, BottomNavItem.Dashboard, BottomNavItem.FollowUps, BottomNavItem.SmartSearch, BottomNavItem.Reports, BottomNavItem.Sync, BottomNavItem.Users, BottomNavItem.Audit, BottomNavItem.Security, BottomNavItem.Onboarding, BottomNavItem.Performance, BottomNavItem.ImportExport, BottomNavItem.Gallery)

            if (currentRoute in items.map { it.route }) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            composable(BottomNavItem.Contacts.route) {
                ContactsScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.Doctors.route) {
                DoctorsScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.Gallery.route) {
                GalleryScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.ImportExport.route) {
                ImportExportScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.FollowUps.route) {
                FollowUpsScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.Reports.route) {
                ReportsScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.Security.route) {
                SecuritySettingsScreen(navController = navController)
            }
            composable(BottomNavItem.Onboarding.route) {
                OnboardingPermissionsScreen(navController = navController)
            }
            composable(BottomNavItem.Sync.route) {
                CloudSyncScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.Users.route) {
                UserManagementScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.Audit.route) {
                AuditLogScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.SmartSearch.route) {
                SmartSearchScreen(viewModel = viewModel, navController = navController)
            }
            composable(BottomNavItem.Performance.route) {
                ArchitecturePerformanceScreen(viewModel = viewModel, navController = navController)
            }
            composable(
                route = "add_edit_client/{clientId}?phone={phone}",
                arguments = listOf(
                    navArgument("clientId") { type = NavType.LongType },
                    navArgument("phone") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                AddEditClientScreen(
                    viewModel = viewModel,
                    navController = navController,
                    clientId = clientId,
                    incomingPhone = phone
                )
            }
            composable("privacy_security") {
                PrivacyScreen(navController = navController)
            }
            composable(
                route = "client_details/{clientId}",
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) { backStackEntry ->
                ClientDetailsScreen(viewModel = viewModel, navController = navController, clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L)
            }
            composable(
                route = "bulk_message?specialization={specialization}&location={location}",
                arguments = listOf(
                    navArgument("specialization") { type = NavType.StringType; defaultValue = "" },
                    navArgument("location") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val specialization = backStackEntry.arguments?.getString("specialization") ?: ""
                val location = backStackEntry.arguments?.getString("location") ?: ""
                BulkMessageScreen(
                    viewModel = viewModel,
                    navController = navController,
                    specialization = specialization,
                    location = location
                )
            }
        }
    }
}
