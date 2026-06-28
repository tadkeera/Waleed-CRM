package com.waleed.crm.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.MedicalServices
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

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Contacts : BottomNavItem("contacts_list", "قائمة الأسماء", Icons.Default.Contacts)
    object Doctors : BottomNavItem("doctors_list", "الأطباء", Icons.Default.MedicalServices)
    object Gallery : BottomNavItem("media_gallery", "المعرض", Icons.Default.Collections)
    object Dashboard : BottomNavItem("analytics_dashboard", "داشبورد تحليلي", Icons.Default.Analytics)
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
            val items = listOf(BottomNavItem.Contacts, BottomNavItem.Doctors, BottomNavItem.Gallery, BottomNavItem.Dashboard)

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
