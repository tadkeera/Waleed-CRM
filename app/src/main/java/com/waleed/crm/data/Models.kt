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
