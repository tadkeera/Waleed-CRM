package com.waleed.crm.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["client_type", "name"]),
        Index(value = ["phone"]),
        Index(value = ["specialization"]),
        Index(value = ["location"]),
        Index(value = ["client_class"])
    ]
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    @ColumnInfo(name = "second_phone") val secondPhone: String = "",
    @ColumnInfo(name = "client_type") val clientType: String = "طبيب",
    val specialization: String = "",
    @ColumnInfo(name = "client_class") val clientClass: String = "B",
    val location: String = "",
    @ColumnInfo(name = "is_classified") val isClassified: Boolean = false,
    @ColumnInfo(name = "card_color") val cardColor: String = "#2196F3",
    @ColumnInfo(name = "date_added") val dateAdded: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "message_logs", indices = [Index(value = ["client_id", "timestamp"]), Index(value = ["campaign_id"])])
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "client_id") val clientId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "message_text") val messageText: String = "",
    @ColumnInfo(name = "attachment_name") val attachmentName: String = "",
    @ColumnInfo(name = "attachment_type") val attachmentType: String = "",
    @ColumnInfo(name = "send_mode") val sendMode: String = "TEXT_ONLY",
    @ColumnInfo(name = "campaign_id") val campaignId: Long = 0,
    val status: String = "OPENED"
)

@Entity(tableName = "message_campaigns", indices = [Index(value = ["date_created"])])
data class MessageCampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "target_count") val targetCount: Int,
    @ColumnInfo(name = "sent_count") val sentCount: Int = 0,
    @ColumnInfo(name = "message_mode") val messageMode: String = "TEXT_ONLY",
    @ColumnInfo(name = "attachment_name") val attachmentName: String = "",
    @ColumnInfo(name = "date_created") val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "follow_ups", indices = [Index(value = ["status", "due_at"]), Index(value = ["client_id"])])
data class FollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "client_id") val clientId: Long,
    val title: String,
    @ColumnInfo(name = "due_at") val dueAt: Long,
    val status: String = "PENDING",
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val username: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val role: String = "USER",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_login") val lastLogin: Long = 0
)

@Entity(tableName = "audit_logs", indices = [Index(value = ["created_at"]), Index(value = ["action", "entity_type"])])
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String = "system",
    val action: String,
    @ColumnInfo(name = "entity_type") val entityType: String = "APP",
    @ColumnInfo(name = "entity_id") val entityId: Long = 0,
    @ColumnInfo(name = "entity_name") val entityName: String = "",
    val details: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_segments", indices = [Index(value = ["created_at"])])
data class SavedSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val query: String = "",
    @ColumnInfo(name = "client_type") val clientType: String = "الكل",
    val specialization: String = "الكل",
    val location: String = "الكل",
    @ColumnInfo(name = "client_class") val clientClass: String = "الكل",
    @ColumnInfo(name = "only_pending_followup") val onlyPendingFollowUp: Boolean = false,
    @ColumnInfo(name = "only_overdue_followup") val onlyOverdueFollowUp: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
