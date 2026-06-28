package com.waleed.crm.data

data class Client(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val secondPhone: String = "",
    val clientType: String = "طبيب", // طبيب، صيدلي، مدير مشتريات
    val specialization: String = "",
    val clientClass: String = "B", // A, B, C
    val location: String = "",
    val isClassified: Boolean = false,
    val cardColor: String = "#2196F3",
    val dateAdded: Long = System.currentTimeMillis()
)

fun String.withDoctorPrefix(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return trimmed
    val withoutCommonPrefix = trimmed
        .removePrefix("دكتور ")
        .removePrefix("الدكتور ")
        .trim()
    return if (withoutCommonPrefix.startsWith("د.")) {
        withoutCommonPrefix
    } else {
        "د. $withoutCommonPrefix"
    }
}

fun String.withYemenPhoneCode(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return "+967"
    val compact = trimmed.replace(" ", "")
    return when {
        compact.startsWith("+967") -> compact
        compact.startsWith("00967") -> "+967" + compact.removePrefix("00967")
        compact.startsWith("967") -> "+" + compact
        compact.startsWith("0") -> "+967" + compact.drop(1)
        compact.startsWith("+") -> compact
        else -> "+967$compact"
    }
}

fun Client.normalizedForSaving(): Client {
    return copy(
        name = if (clientType == "طبيب") name.withDoctorPrefix() else name.trim(),
        phone = phone.withYemenPhoneCode(),
        secondPhone = if (secondPhone.isBlank()) "" else secondPhone.withYemenPhoneCode()
    )
}


data class Specialization(
    val id: Long = 0,
    val name: String,
    val color: String
)

data class Location(
    val id: Long = 0,
    val name: String
)

data class Pharmacy(
    val id: Long = 0,
    val name: String,
    val clientId: Long
)

data class GalleryFile(
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val type: String, // "image" or "pdf"
    val dateAdded: Long = System.currentTimeMillis()
)

data class MessageLog(
    val id: Long = 0,
    val clientId: Long,
    val timestamp: Long
)

data class DoctorMessageCount(
    val client: Client,
    val messageCount: Int
)
