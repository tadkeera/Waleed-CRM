package com.pharmacomm.crm.data.local

import androidx.room.TypeConverter
import com.pharmacomm.crm.domain.model.ClientType
import com.pharmacomm.crm.domain.model.ImportanceClass
import com.pharmacomm.crm.domain.model.ContactMethod
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromClientType(value: ClientType): String = value.name

    @TypeConverter
    fun toClientType(value: String): ClientType = ClientType.valueOf(value)

    @TypeConverter
    fun fromImportanceClass(value: ImportanceClass): String = value.name

    @TypeConverter
    fun toImportanceClass(value: String): ImportanceClass = ImportanceClass.valueOf(value)

    @TypeConverter
    fun fromContactMethod(value: ContactMethod): String = value.name

    @TypeConverter
    fun toContactMethod(value: String): ContactMethod = ContactMethod.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
}