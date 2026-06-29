package com.waleed.crm.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.waleed.crm.MainActivity
import com.waleed.crm.R
import com.waleed.crm.data.FollowUpWithClient

object FollowUpReminderScheduler {
    const val CHANNEL_ID = "follow_up_reminders"
    private const val ACTION_REMIND = "com.waleed.crm.ACTION_FOLLOW_UP_REMINDER"
    private const val EXTRA_ID = "follow_up_id"
    private const val EXTRA_CLIENT_ID = "client_id"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_CLIENT_NAME = "client_name"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات المتابعات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "إشعارات مواعيد متابعة الأطباء والعملاء" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context, item: FollowUpWithClient) {
        if (item.followUp.status != "PENDING") return
        ensureChannel(context)
        val triggerAt = item.followUp.dueAt.coerceAtLeast(System.currentTimeMillis() + 10_000)
        val intent = Intent(context, FollowUpReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_ID, item.followUp.id)
            putExtra(EXTRA_CLIENT_ID, item.followUp.clientId)
            putExtra(EXTRA_TITLE, item.followUp.title)
            putExtra(EXTRA_CLIENT_NAME, item.client?.name ?: "عميل")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            item.followUp.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 15 * 60 * 1000L, pi)
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context, followUpId: Long) {
        val intent = Intent(context, FollowUpReminderReceiver::class.java).apply { action = ACTION_REMIND }
        val pi = PendingIntent.getBroadcast(
            context,
            followUpId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
        pi.cancel()
    }

    fun showNotification(context: Context, followUpId: Long, clientId: Long, title: String, clientName: String) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_follow_ups", true)
            putExtra("client_id", clientId)
        }
        val openPi = PendingIntent.getActivity(
            context,
            followUpId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("موعد متابعة: $title")
            .setContentText(clientName)
            .setStyle(NotificationCompat.BigTextStyle().bigText("حان وقت متابعة $clientName\n$title"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(followUpId.toInt(), notification) }
    }

    fun extract(intent: Intent): ReminderPayload = ReminderPayload(
        id = intent.getLongExtra(EXTRA_ID, 0L),
        clientId = intent.getLongExtra(EXTRA_CLIENT_ID, 0L),
        title = intent.getStringExtra(EXTRA_TITLE) ?: "متابعة",
        clientName = intent.getStringExtra(EXTRA_CLIENT_NAME) ?: "عميل"
    )
}

data class ReminderPayload(val id: Long, val clientId: Long, val title: String, val clientName: String)
