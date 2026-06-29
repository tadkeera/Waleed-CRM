package com.waleed.crm.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FollowUpReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = FollowUpReminderScheduler.extract(intent)
        if (payload.id > 0L) {
            FollowUpReminderScheduler.showNotification(context, payload.id, payload.clientId, payload.title, payload.clientName)
        }
    }
}
