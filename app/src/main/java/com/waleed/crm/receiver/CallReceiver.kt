package com.waleed.crm.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.waleed.crm.MainActivity
import com.waleed.crm.data.CrmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "crm_call_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            if (state == TelephonyManager.EXTRA_STATE_RINGING && !incomingNumber.isNullOrBlank()) {
                val repository = CrmRepository(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val existing = repository.getClientByPhone(incomingNumber)
                    if (existing == null) {
                        showNotification(context, incomingNumber)
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, phone: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "إشعارات المكالمات غير المحفوظة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه لحفظ وتصنيف الأرقام الجديدة غير المحفوظة في قاعدة بيانات CRM"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("incoming_phone", phone)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            phone.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("رقم جديد غير محفوظ ($phone)")
            .setContentText("انقر لحفظ وتصنيف العميل (طبيب، صيدلي، مدير مشتريات) وإكمال بياناته.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
