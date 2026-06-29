package com.waleed.crm.data.room

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.waleed.crm.data.DatabaseHelper
import com.waleed.crm.data.SeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

/**
 * Phase 24 legacy bridge: SQLiteOpenHelper is no longer part of normal reads/writes.
 * It is opened only once, only when a historical SQLite database exists and the Room migration
 * marker is still missing. The migration then validates table counts and writes an audit record.
 */
object LegacyDataMigrator {
    private const val PREFS = "waleed_room_migration"
    private const val KEY_DONE = "phase23_sqlite_to_room_done_v1"
    private const val KEY_STATUS = "phase24_integrity_status"
    private const val KEY_DETAILS = "phase24_integrity_details"
    private const val KEY_COMPLETED_AT = "phase24_completed_at"

    data class TableCounts(
        val clients: Int = 0,
        val specializations: Int = 0,
        val locations: Int = 0,
        val pharmacies: Int = 0,
        val galleryFiles: Int = 0,
        val messageTemplates: Int = 0,
        val messageLogs: Int = 0,
        val messageCampaigns: Int = 0,
        val followUps: Int = 0,
        val users: Int = 0,
        val auditLogs: Int = 0,
        val savedSegments: Int = 0
    ) {
        fun comparableWithoutAudit(): Map<String, Int> = mapOf(
            "clients" to clients,
            "specializations" to specializations,
            "locations" to locations,
            "pharmacies" to pharmacies,
            "gallery_files" to galleryFiles,
            "message_templates" to messageTemplates,
            "message_logs" to messageLogs,
            "message_campaigns" to messageCampaigns,
            "follow_ups" to followUps,
            "users" to users,
            "saved_segments" to savedSegments
        )
    }

    data class MigrationStatus(
        val completed: Boolean,
        val status: String,
        val details: String,
        val completedAt: Long
    )

    fun status(context: Context): MigrationStatus {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return MigrationStatus(
            completed = prefs.getBoolean(KEY_DONE, false),
            status = prefs.getString(KEY_STATUS, "PENDING") ?: "PENDING",
            details = prefs.getString(KEY_DETAILS, "") ?: "",
            completedAt = prefs.getLong(KEY_COMPLETED_AT, 0L)
        )
    }

    suspend fun migrateIfNeeded(context: Context, db: WaleedRoomDatabase) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return@withContext

        val legacyPath = appContext.getDatabasePath(DatabaseHelper.DATABASE_NAME)
        if (!legacyPath.exists()) {
            seedFreshRoomDatabase(db)
            val roomCounts = roomCounts(db)
            val details = "Fresh Room seed completed: $roomCounts"
            db.auditDao().insert(AuditLogEntity(action = "ROOM_FRESH_SEED", entityType = "DATABASE", details = details))
            prefs.edit()
                .putBoolean(KEY_DONE, true)
                .putString(KEY_STATUS, "SEEDED")
                .putString(KEY_DETAILS, details)
                .putLong(KEY_COMPLETED_AT, System.currentTimeMillis())
                .apply()
            return@withContext
        }

        val legacy = DatabaseHelper(appContext).readableDatabase
        val legacyCounts = legacyCounts(legacy)
        try {
            db.withTransaction {
                db.catalogDao().insertSpecializations(readSpecializations(legacy.query(DatabaseHelper.TABLE_SPECIALIZATIONS, null, null, null, null, null, null)))
                db.catalogDao().insertLocations(readLocations(legacy.query(DatabaseHelper.TABLE_LOCATIONS, null, null, null, null, null, null)))
                db.clientDao().insertAll(readClients(legacy.query(DatabaseHelper.TABLE_CLIENTS, null, null, null, null, null, null)))
                db.catalogDao().insertPharmacies(readPharmacies(legacy.query(DatabaseHelper.TABLE_PHARMACIES, null, null, null, null, null, null)))
                db.galleryDao().insertAll(readGalleryFiles(legacy.query(DatabaseHelper.TABLE_GALLERY_FILES, null, null, null, null, null, null)))
                db.messageDao().insertTemplates(readMessageTemplates(legacy.query(DatabaseHelper.TABLE_MESSAGE_TEMPLATES, null, null, null, null, null, null)))
                db.messageDao().insertCampaigns(readMessageCampaigns(legacy.query(DatabaseHelper.TABLE_MESSAGE_CAMPAIGNS, null, null, null, null, null, null)))
                db.messageDao().insertLogs(readMessageLogs(legacy.query(DatabaseHelper.TABLE_MESSAGE_LOGS, null, null, null, null, null, null)))
                db.followUpDao().insertAll(readFollowUps(legacy.query(DatabaseHelper.TABLE_FOLLOW_UPS, null, null, null, null, null, null)))
                db.userDao().insertAll(readUsers(legacy.query(DatabaseHelper.TABLE_USERS, null, null, null, null, null, null)))
                db.auditDao().insertAll(readAuditLogs(legacy.query(DatabaseHelper.TABLE_AUDIT_LOGS, null, null, null, null, null, null)))
                db.segmentDao().insertAll(readSegments(legacy.query(DatabaseHelper.TABLE_SAVED_SEGMENTS, null, null, null, null, null, null)))
            }
            val roomCounts = roomCounts(db)
            val mismatches = compareCounts(legacyCounts, roomCounts)
            val status = if (mismatches.isEmpty()) "OK" else "MISMATCH"
            val details = if (mismatches.isEmpty()) {
                "SQLite to Room integrity check passed. legacy=$legacyCounts room=$roomCounts"
            } else {
                "SQLite to Room integrity check mismatches: ${mismatches.joinToString()}. legacy=$legacyCounts room=$roomCounts"
            }
            db.auditDao().insert(AuditLogEntity(action = "ROOM_MIGRATION_$status", entityType = "DATABASE", details = details))
            prefs.edit()
                .putBoolean(KEY_DONE, true)
                .putString(KEY_STATUS, status)
                .putString(KEY_DETAILS, details)
                .putLong(KEY_COMPLETED_AT, System.currentTimeMillis())
                .apply()
        } catch (t: Throwable) {
            val details = "SQLite to Room migration failed: ${t.message ?: t::class.java.simpleName}"
            db.auditDao().insert(AuditLogEntity(action = "ROOM_MIGRATION_FAILED", entityType = "DATABASE", details = details))
            prefs.edit()
                .putString(KEY_STATUS, "FAILED")
                .putString(KEY_DETAILS, details)
                .putLong(KEY_COMPLETED_AT, System.currentTimeMillis())
                .apply()
            throw t
        } finally {
            legacy.close()
        }
    }

    private fun compareCounts(legacy: TableCounts, room: TableCounts): List<String> {
        val legacyMap = legacy.comparableWithoutAudit()
        val roomMap = room.comparableWithoutAudit()
        return legacyMap.keys.mapNotNull { key ->
            val legacyCount = legacyMap[key] ?: 0
            val roomCount = roomMap[key] ?: 0
            if (legacyCount == roomCount) null else "$key legacy=$legacyCount room=$roomCount"
        }
    }

    private fun legacyCounts(db: SQLiteDatabase) = TableCounts(
        clients = countTable(db, DatabaseHelper.TABLE_CLIENTS),
        specializations = countTable(db, DatabaseHelper.TABLE_SPECIALIZATIONS),
        locations = countTable(db, DatabaseHelper.TABLE_LOCATIONS),
        pharmacies = countTable(db, DatabaseHelper.TABLE_PHARMACIES),
        galleryFiles = countTable(db, DatabaseHelper.TABLE_GALLERY_FILES),
        messageTemplates = countTable(db, DatabaseHelper.TABLE_MESSAGE_TEMPLATES),
        messageLogs = countTable(db, DatabaseHelper.TABLE_MESSAGE_LOGS),
        messageCampaigns = countTable(db, DatabaseHelper.TABLE_MESSAGE_CAMPAIGNS),
        followUps = countTable(db, DatabaseHelper.TABLE_FOLLOW_UPS),
        users = countTable(db, DatabaseHelper.TABLE_USERS),
        auditLogs = countTable(db, DatabaseHelper.TABLE_AUDIT_LOGS),
        savedSegments = countTable(db, DatabaseHelper.TABLE_SAVED_SEGMENTS)
    )

    private suspend fun roomCounts(db: WaleedRoomDatabase) = TableCounts(
        clients = db.clientDao().count(),
        specializations = db.catalogDao().countSpecializations(),
        locations = db.catalogDao().countLocations(),
        pharmacies = db.catalogDao().countPharmacies(),
        galleryFiles = db.galleryDao().count(),
        messageTemplates = db.messageDao().countTemplates(),
        messageLogs = db.messageDao().countLogs(),
        messageCampaigns = db.messageDao().countCampaigns(),
        followUps = db.followUpDao().count(),
        users = db.userDao().count(),
        auditLogs = db.auditDao().count(),
        savedSegments = db.segmentDao().count()
    )

    private fun countTable(db: SQLiteDatabase, table: String): Int = try {
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    } catch (_: Throwable) {
        0
    }

    private suspend fun seedFreshRoomDatabase(db: WaleedRoomDatabase) {
        val colorsPool = listOf(
            "#E57373", "#81C784", "#64B5F6", "#BA68C8", "#FFB74D", "#4DD0E1",
            "#F06292", "#AED581", "#FF8A65", "#9575CD", "#4DB6AC", "#DCE775"
        )
        val defaultSpecs = listOf(
            "قلب وأوعية دموية" to "#E57373", "أطفال" to "#81C784", "باطنية" to "#64B5F6",
            "نساء وتوليد" to "#BA68C8", "عظام" to "#FFB74D", "عيون" to "#4DD0E1",
            "جلدية" to "#F06292", "أنف وأذن وحنجرة" to "#AED581"
        ).map { SpecializationEntity(name = it.first, color = it.second) }
        val defaultLocs = listOf("وسط المدينة", "شمال المدينة", "جنوب المدينة", "شرق المدينة", "غرب المدينة").map { LocationEntity(name = it) }
        db.withTransaction {
            db.catalogDao().insertSpecializations(defaultSpecs)
            db.catalogDao().insertLocations(defaultLocs)
            val doctors = SeedData.doctors.map { doctor ->
                val color = if (doctor.specialization.isBlank()) doctor.cardColor else colorsPool[Math.floorMod(doctor.specialization.hashCode(), colorsPool.size)]
                ClientEntity(
                    name = doctor.name,
                    phone = doctor.phone,
                    secondPhone = doctor.secondPhone,
                    clientType = doctor.clientType,
                    specialization = doctor.specialization,
                    clientClass = doctor.clientClass,
                    location = doctor.location,
                    isClassified = true,
                    cardColor = color,
                    dateAdded = doctor.dateAdded,
                    updatedAt = doctor.updatedAt,
                    notes = doctor.notes
                )
            }
            db.clientDao().insertAll(doctors)
            db.catalogDao().insertSpecializations(SeedData.doctors.filter { it.specialization.isNotBlank() }.map {
                SpecializationEntity(name = it.specialization, color = colorsPool[Math.floorMod(it.specialization.hashCode(), colorsPool.size)])
            })
            db.catalogDao().insertLocations(SeedData.doctors.filter { it.location.isNotBlank() }.map { LocationEntity(name = it.location) })
        }
    }

    private fun readClients(c: Cursor) = c.use {
        buildList {
            while (it.moveToNext()) add(ClientEntity(
                id = it.long("id"), name = it.string("name"), phone = it.string("phone"), secondPhone = it.string("second_phone"),
                clientType = it.string("client_type", "طبيب"), specialization = it.string("specialization"), clientClass = it.string("client_class", "B"),
                location = it.string("location"), isClassified = it.int("is_classified") == 1, cardColor = it.string("card_color", "#2196F3"),
                dateAdded = it.long("date_added", System.currentTimeMillis()), updatedAt = it.long("updated_at"), notes = it.string("notes")
            ))
        }
    }
    private fun readSpecializations(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(SpecializationEntity(it.long("id"), it.string("name"), it.string("color", "#2196F3"))) } }
    private fun readLocations(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(LocationEntity(it.long("id"), it.string("name"))) } }
    private fun readPharmacies(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(PharmacyEntity(it.long("id"), it.string("name"), it.long("client_id"))) } }
    private fun readGalleryFiles(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(GalleryFileEntity(it.long("id"), it.string("name"), it.string("file_path"), it.string("type"), it.long("date_added"))) } }
    private fun readMessageTemplates(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(MessageTemplateEntity(it.long("id"), it.string("title"), it.string("body"), it.long("date_added"))) } }
    private fun readMessageCampaigns(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(MessageCampaignEntity(it.long("id"), it.string("title"), it.int("target_count"), it.int("sent_count"), it.string("message_mode", "TEXT_ONLY"), it.string("attachment_name"), it.long("date_created"))) } }
    private fun readMessageLogs(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(MessageLogEntity(it.long("id"), it.long("client_id"), it.long("timestamp"), it.string("message_text"), it.string("attachment_name"), it.string("attachment_type"), it.string("send_mode", "TEXT_ONLY"), it.long("campaign_id"), it.string("status", "OPENED"))) } }
    private fun readFollowUps(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(FollowUpEntity(it.long("id"), it.long("client_id"), it.string("title"), it.long("due_at"), it.string("status", "PENDING"), it.string("notes"), it.long("created_at"))) } }
    private fun readUsers(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(UserEntity(it.long("id"), it.string("name"), it.string("username"), it.string("password_hash"), it.string("role", "USER"), it.int("is_active", 1) == 1, it.long("created_at"), it.long("last_login"))) } }
    private fun readAuditLogs(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(AuditLogEntity(it.long("id"), it.string("username", "system"), it.string("action"), it.string("entity_type", "APP"), it.long("entity_id"), it.string("entity_name"), it.string("details"), it.long("created_at"))) } }
    private fun readSegments(c: Cursor) = c.use { buildList { while (it.moveToNext()) add(SavedSegmentEntity(it.long("id"), it.string("name"), it.string("query"), it.string("client_type", "الكل"), it.string("specialization", "الكل"), it.string("location", "الكل"), it.string("client_class", "الكل"), it.int("only_pending_followup") == 1, it.int("only_overdue_followup") == 1, it.long("created_at"))) } }

    private fun Cursor.string(column: String, default: String = ""): String {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getString(idx) ?: default else default
    }
    private fun Cursor.long(column: String, default: Long = 0L): Long {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getLong(idx) else default
    }
    private fun Cursor.int(column: String, default: Int = 0): Int {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getInt(idx) else default
    }
}
