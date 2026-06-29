package com.waleed.crm.data

import java.text.Normalizer

data class Client(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val secondPhone: String = "",
    val clientType: String = "طبيب",
    val specialization: String = "",
    val clientClass: String = "B",
    val location: String = "",
    val isClassified: Boolean = false,
    val cardColor: String = "#2196F3",
    val dateAdded: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

fun String.stripDoctorPrefix(): String = trim()
    .removePrefix("الدكتور ")
    .removePrefix("دكتور ")
    .removePrefix("د. ")
    .removePrefix("د.")
    .trim()

fun String.withDoctorPrefix(): String {
    val trimmed = stripDoctorPrefix()
    if (trimmed.isBlank()) return trimmed
    return "د. $trimmed"
}

fun String.doctorDisplayName(clientType: String = "طبيب"): String = if (clientType == "طبيب") withDoctorPrefix() else trim()

fun String.normalizedArabicSearchKey(): String = Normalizer.normalize(stripDoctorPrefix(), Normalizer.Form.NFD)
    .replace("[\\u064B-\\u065F]".toRegex(), "")
    .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
    .replace("ى", "ي").replace("ة", "ه").replace("ؤ", "و").replace("ئ", "ي")
    .replace("ـ", "").replace(" ", "").trim().lowercase()

fun String.withYemenPhoneCode(): String {
    val compact = trim().replace(" ", "").replace("-", "")
    if (compact.isBlank()) return "+967"
    return when {
        compact.startsWith("+967") -> compact
        compact.startsWith("00967") -> "+967" + compact.removePrefix("00967")
        compact.startsWith("967") -> "+$compact"
        compact.startsWith("0") -> "+967" + compact.drop(1)
        compact.startsWith("+") -> compact
        else -> "+967$compact"
    }
}

fun Client.normalizedForSaving(): Client {
    val now = System.currentTimeMillis()
    return copy(
        name = if (clientType == "طبيب") name.stripDoctorPrefix() else name.trim(),
        phone = phone.withYemenPhoneCode(),
        secondPhone = if (secondPhone.isBlank() || secondPhone == "+967") "" else secondPhone.withYemenPhoneCode(),
        updatedAt = now,
        notes = notes.trim()
    )
}

data class Specialization(val id: Long = 0, val name: String, val color: String)
data class Location(val id: Long = 0, val name: String)
data class Pharmacy(val id: Long = 0, val name: String, val clientId: Long)
data class GalleryFile(val id: Long = 0, val name: String, val filePath: String, val type: String, val dateAdded: Long = System.currentTimeMillis())
data class MessageTemplate(val id: Long = 0, val title: String, val body: String, val dateAdded: Long = System.currentTimeMillis())
data class MessageCampaign(val id: Long = 0, val title: String, val targetCount: Int, val sentCount: Int = 0, val messageMode: String = "TEXT_ONLY", val attachmentName: String = "", val dateCreated: Long = System.currentTimeMillis())
data class MessageLog(
    val id: Long = 0,
    val clientId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val messageText: String = "",
    val attachmentName: String = "",
    val attachmentType: String = "",
    val sendMode: String = "TEXT_ONLY",
    val campaignId: Long = 0,
    val status: String = "OPENED"
)
data class DoctorMessageCount(val client: Client, val messageCount: Int)


data class FollowUp(
    val id: Long = 0,
    val clientId: Long,
    val title: String,
    val dueAt: Long,
    val status: String = "PENDING",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class FollowUpWithClient(
    val followUp: FollowUp,
    val client: Client?
)
