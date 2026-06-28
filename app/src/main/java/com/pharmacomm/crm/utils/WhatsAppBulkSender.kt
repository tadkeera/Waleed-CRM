package com.pharmacomm.crm.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.ContactLog
import com.pharmacomm.crm.domain.model.ContactMethod
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

object WhatsAppBulkSender {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"

    suspend fun sendBulkMessages(
        context: Context,
        clients: List<Client>,
        template: String,
        onComplete: (Boolean) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                for (i in clients.indices) {
                    val client = clients[i]
                    val phones = AppContainer.clientRepository.getPhoneNumbersByClientSync(client.id)
                    val phone = phones.firstOrNull()?.number?.replace(Regex("[^0-9+]"), "") ?: continue

                    val personalizedMessage = template.replace("{name}", client.name)

                    // Open WhatsApp for this contact
                    sendSingleWhatsAppMessage(context, phone, personalizedMessage)

                    // Log the contact
                    AppContainer.contactLogRepository.insertLog(
                        ContactLog(
                            clientId = client.id,
                            method = ContactMethod.WHATSAPP,
                            message = personalizedMessage.take(100)
                        )
                    )

                    // Update client contact info
                    AppContainer.clientRepository.updateContactInfo(client.id)

                    // Human-like random delay 10-15 seconds
                    if (i < clients.lastIndex) {
                        val delayMs = Random.nextLong(10_000, 15_000)
                        delay(delayMs)
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في الإرسال: ${e.message}", Toast.LENGTH_LONG).show()
                onComplete(false)
            }
        }
    }

    fun sendSingleMessage(context: Context, client: Client, template: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val phones = AppContainer.clientRepository.getPhoneNumbersByClientSync(client.id)
            val phone = phones.firstOrNull()?.number?.replace(Regex("[^0-9+]"), "") ?: return@launch

            val personalizedMessage = template.replace("{name}", client.name)

            sendSingleWhatsAppMessage(context, phone, personalizedMessage)

            AppContainer.contactLogRepository.insertLog(
                ContactLog(
                    clientId = client.id,
                    method = ContactMethod.WHATSAPP,
                    message = personalizedMessage.take(100)
                )
            )
            AppContainer.clientRepository.updateContactInfo(client.id)
        }
    }

    private fun sendSingleWhatsAppMessage(context: Context, phone: String, message: String) {
        try {
            // Build WhatsApp Intent
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Try to open WhatsApp
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: open browser WhatsApp web
                val fallback = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(fallback)
            }
        } catch (e: Exception) {
            // General fallback
            val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        }
    }
}