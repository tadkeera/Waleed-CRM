package com.pharmacomm.crm.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "contact_logs",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ContactLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val timestamp: Date = Date(),
    val method: ContactMethod,
    val message: String? = null
)

enum class ContactMethod {
    WHATSAPP, CALL, SMS, OTHER
}