package com.waleed.crm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.waleed.crm.ui.navigation.AppNavigation
import com.waleed.crm.ui.navigation.BottomNavItem
import com.waleed.crm.ui.viewmodel.CrmViewModel
import com.waleed.crm.ui.viewmodel.CrmViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CrmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, CrmViewModelFactory(this))[CrmViewModel::class.java]

        // Request runtime permissions for Phone State and Notifications
        requestPermissionsIfNeeded()

        // Check if opened via CallReceiver notification
        val incomingPhone = intent.getStringExtra("incoming_phone") ?: ""
        val initialRoute = if (incomingPhone.isNotBlank()) {
            "add_edit_client/0?phone=$incomingPhone"
        } else {
            BottomNavItem.Contacts.route
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    AppNavigation(viewModel = viewModel, initialRoute = initialRoute, navController = navController)
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
                // Permissions handled
            }
            launcher.launch(neededPermissions.toTypedArray())
        }
    }
}
