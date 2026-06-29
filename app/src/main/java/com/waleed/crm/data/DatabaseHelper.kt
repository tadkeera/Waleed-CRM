package com.waleed.crm.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "waleed_crm.db"
        const val DATABASE_VERSION = 6

        // Tables
        const val TABLE_CLIENTS = "clients"
        const val TABLE_SPECIALIZATIONS = "specializations"
        const val TABLE_LOCATIONS = "locations"
        const val TABLE_PHARMACIES = "pharmacies"
        const val TABLE_GALLERY_FILES = "gallery_files"
        const val TABLE_MESSAGE_LOGS = "message_logs"
        const val TABLE_MESSAGE_TEMPLATES = "message_templates"
        const val TABLE_MESSAGE_CAMPAIGNS = "message_campaigns"
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
                date_added INTEGER,
                updated_at INTEGER,
                notes TEXT DEFAULT ''
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
                message_text TEXT DEFAULT '',
                attachment_name TEXT DEFAULT '',
                attachment_type TEXT DEFAULT '',
                send_mode TEXT DEFAULT 'TEXT_ONLY',
                campaign_id INTEGER DEFAULT 0,
                status TEXT DEFAULT 'OPENED',
                FOREIGN KEY(client_id) REFERENCES $TABLE_CLIENTS(id) ON DELETE CASCADE
            )
        """.trimIndent())

        createMessagingTables(db)
        createIndexes(db)

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
                put("updated_at", doctor.updatedAt)
                put("notes", doctor.notes)
            }
            db.insert(TABLE_CLIENTS, null, cv)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        addColumnIfMissing(db, TABLE_CLIENTS, "updated_at", "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_CLIENTS, "notes", "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_MESSAGE_LOGS, "message_text", "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_MESSAGE_LOGS, "attachment_name", "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_MESSAGE_LOGS, "attachment_type", "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_MESSAGE_LOGS, "send_mode", "TEXT DEFAULT 'TEXT_ONLY'")
        addColumnIfMissing(db, TABLE_MESSAGE_LOGS, "campaign_id", "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_MESSAGE_LOGS, "status", "TEXT DEFAULT 'OPENED'")
        createMessagingTables(db)
        createIndexes(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        createMessagingTables(db)
        createIndexes(db)
    }

    private fun addColumnIfMissing(db: SQLiteDatabase, table: String, column: String, definition: String) {
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            var exists = false
            while (c.moveToNext()) if (c.getString(c.getColumnIndexOrThrow("name")) == column) exists = true
            if (!exists) db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
        }
    }

    private fun createMessagingTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGE_TEMPLATES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                date_added INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGE_CAMPAIGNS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                target_count INTEGER DEFAULT 0,
                sent_count INTEGER DEFAULT 0,
                message_mode TEXT DEFAULT 'TEXT_ONLY',
                attachment_name TEXT DEFAULT '',
                date_created INTEGER NOT NULL
            )
        """.trimIndent())
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_clients_type_name ON $TABLE_CLIENTS(client_type, name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_clients_phone ON $TABLE_CLIENTS(phone)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_logs_client_time ON $TABLE_MESSAGE_LOGS(client_id, timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_logs_campaign ON $TABLE_MESSAGE_LOGS(campaign_id)")
    }
}
