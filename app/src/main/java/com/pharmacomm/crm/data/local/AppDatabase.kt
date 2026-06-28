package com.pharmacomm.crm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pharmacomm.crm.domain.model.*

@Database(
    entities = [
        Client::class,
        PhoneNumber::class,
        Specialty::class,
        Region::class,
        ContactLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun phoneNumberDao(): PhoneNumberDao
    abstract fun lookupDao(): LookupDao
    abstract fun contactLogDao(): ContactLogDao
}