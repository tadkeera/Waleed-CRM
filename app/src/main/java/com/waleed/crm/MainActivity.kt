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
import com.waleed.crm.reminders.FollowUpReminderScheduler
import com.waleed.crm.security.AppLockScreen
import com.waleed.crm.security.savedPin
import com.waleed.crm.ui.screens.isOnboardingDone
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CrmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, CrmViewModelFactory(this))[CrmViewModel::class.java]
        FollowUpReminderScheduler.ensureChannel(this)

        // Request runtime permissions for Phone State and Notifications
        requestPermissionsIfNeeded()

        // Check if opened via CallReceiver notification
        val incomingPhone = intent.getStringExtra("incoming_phone") ?: ""
        val openFollowUps = intent.getBooleanExtra("open_follow_ups", false)
        val notificationClientId = intent.getLongExtra("client_id", 0L)
        val initialRoute = when {
            incomingPhone.isNotBlank() -> "add_edit_client/0?phone=$incomingPhone"
            openFollowUps && notificationClientId > 0L -> "client_details/$notificationClientId"
            openFollowUps -> BottomNavItem.FollowUps.route
            else -> if (isOnboardingDone(this)) BottomNavItem.Contacts.route else BottomNavItem.Onboarding.route
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var unlocked by remember { mutableStateOf(savedPin(this@MainActivity).isBlank()) }
                    if (unlocked) {
                        val navController = rememberNavController()
                        AppNavigation(viewModel = viewModel, initialRoute = initialRoute, navController = navController)
                    } else {
                        AppLockScreen(context = this@MainActivity) { unlocked = true }
                    }
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
                // الصلاحيات اختيارية ويتم التعامل مع الرفض بدون إيقاف التطبيق.
            }
            launcher.launch(neededPermissions.toTypedArray())
        }
    }
}
