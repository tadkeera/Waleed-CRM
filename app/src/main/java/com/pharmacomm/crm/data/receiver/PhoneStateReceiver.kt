package com.pharmacomm.crm.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.pharmacomm.crm.presentation.ui.overlay.CallOverlayService
import kotlinx.coroutines.*

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        if (state == TelephonyManager.EXTRA_STATE_RINGING || state == TelephonyManager.EXTRA_STATE_IDLE) {
            if (!incomingNumber.isNullOrBlank()) {
                // Check if this number is not already in CRM - simple check
                CoroutineScope(Dispatchers.IO).launch {
                    val phones = try {
                        // For demo we always trigger on unknown numbers
                        true
                    } catch (e: Exception) { true }

                    if (phones) {
                        val overlayIntent = Intent(context, CallOverlayService::class.java).apply {
                            putExtra("phone", incomingNumber)
                            putExtra("name", "مكالمة واردة")
                        }
                        context.startService(overlayIntent)
                    }
                }
            }
        }
    }
}