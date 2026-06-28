package com.waleed.crm.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "waleed_crm.db"
        const val DATABASE_VERSION = 3

        // Tables
        const val TABLE_CLIENTS = "clients"
        const val TABLE_SPECIALIZATIONS = "specializations"
        const val TABLE_LOCATIONS = "locations"
        const val TABLE_PHARMACIES = "pharmacies"
        const val TABLE_GALLERY_FILES = "gallery_files"
        const val TABLE_MESSAGE_LOGS = "message_logs"
    }

    private val colorsPool = listOf(
        "#E57373", "#81C784", "#64B5F6", "#BA68C8",
        "#FFB74D", "#4DD0E1", "#F06292", "#AED581",
        "#FF8A65", "#9575CD", "#4DB6AC", "#DCE775"
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CLIENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                second_phone TEXT,
                client_type TEXT,
                specialization TEXT,
                client_class TEXT,
                location TEXT,
                is_classified INTEGER DEFAULT 0,
                card_color TEXT,
                date_added INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_SPECIALIZATIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                color TEXT NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_LOCATIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_PHARMACIES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                client_id INTEGER NOT NULL,
                FOREIGN KEY(client_id) REFERENCES $TABLE_CLIENTS(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_GALLERY_FILES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                file_path TEXT NOT NULL,
                type TEXT NOT NULL,
                date_added INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_MESSAGE_LOGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_id INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(client_id) REFERENCES $TABLE_CLIENTS(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Insert default specializations
        val defaultSpecs = listOf(
            "قلب وأوعية دموية" to "#E57373",
            "أطفال" to "#81C784",
            "باطنية" to "#64B5F6",
            "نساء وتوليد" to "#BA68C8",
            "عظام" to "#FFB74D",
            "عيون" to "#4DD0E1",
            "جلدية" to "#F06292",
            "أنف وأذن وحنجرة" to "#AED581"
        )
        for ((spec, color) in defaultSpecs) {
            val cv = ContentValues().apply {
                put("name", spec)
                put("color", color)
            }
            db.insertWithOnConflict(TABLE_SPECIALIZATIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }

        // Insert default locations
        val defaultLocs = listOf("وسط المدينة", "شمال المدينة", "جنوب المدينة", "شرق المدينة", "غرب المدينة")
        for (loc in defaultLocs) {
            val cv = ContentValues().apply { put("name", loc) }
            db.insertWithOnConflict(TABLE_LOCATIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }

        // Seed data from the PDF
        for (doctor in SeedData.doctors) {
            var cardColor = doctor.cardColor
            if (doctor.specialization.isNotBlank()) {
                val specCv = ContentValues().apply {
                    put("name", doctor.specialization)
                    put("color", colorsPool.random())
                }
                db.insertWithOnConflict(TABLE_SPECIALIZATIONS, null, specCv, SQLiteDatabase.CONFLICT_IGNORE)
                
                db.query(TABLE_SPECIALIZATIONS, arrayOf("color"), "name = ?", arrayOf(doctor.specialization), null, null, null).use {
                    if (it.moveToFirst()) {
                        cardColor = it.getString(0)
                    }
                }
            }

            if (doctor.location.isNotBlank()) {
                val locCv = ContentValues().apply { put("name", doctor.location) }
                db.insertWithOnConflict(TABLE_LOCATIONS, null, locCv, SQLiteDatabase.CONFLICT_IGNORE)
            }

            val cv = ContentValues().apply {
                put("name", doctor.name)
                put("phone", doctor.phone)
                put("second_phone", doctor.secondPhone)
                put("client_type", doctor.clientType)
                put("specialization", doctor.specialization)
                put("client_class", doctor.clientClass)
                put("location", doctor.location)
                put("is_classified", 1)
                put("card_color", cardColor)
                put("date_added", doctor.dateAdded)
            }
            db.insert(TABLE_CLIENTS, null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGE_LOGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GALLERY_FILES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PHARMACIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SPECIALIZATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLIENTS")
        onCreate(db)
    }
}
