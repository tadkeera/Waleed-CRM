package com.pharmacomm.crm.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val clientType: ClientType,
    val importanceClass: ImportanceClass,
    val specialty: String? = null,
    val region: String? = null,
    val notes: String? = null,
    val linkedPharmacies: List<String> = emptyList(),
    val createdAt: Date = Date(),
    val lastContactDate: Date? = null,
    val contactCount: Int = 0
) {
    val isComplete: Boolean
        get() = specialty != null && importanceClass != ImportanceClass.NONE
}

enum class ClientType(val displayName: String) {
    DOCTOR("طبيب"),
    PHARMACIST("صيدلي"),
    PROCUREMENT_MANAGER("مدير مشتريات");

    companion object {
        fun fromDisplayName(name: String): ClientType {
            return values().firstOrNull { it.displayName == name } ?: DOCTOR
        }
    }
}

enum class ImportanceClass(val displayName: String) {
    CLASS_A("Class A"),
    CLASS_B("Class B"),
    CLASS_C("Class C"),
    NONE("غير محدد");

    companion object {
        fun fromDisplayName(name: String): ImportanceClass {
            return values().firstOrNull { it.displayName == name } ?: NONE
        }
    }
}