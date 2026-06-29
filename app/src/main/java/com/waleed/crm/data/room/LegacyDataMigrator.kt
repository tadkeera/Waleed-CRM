package com.waleed.crm.data.room

import android.content.Context
import android.database.Cursor
import com.waleed.crm.data.DatabaseHelper
import com.waleed.crm.data.SeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

/**
 * Phase 23 bridge: copies the user's historical SQLiteOpenHelper data into the Room store once.
 * After this step CrmRepository reads/writes Room only; SQLiteOpenHelper is opened only when an
 * existing legacy database file is detected and only for this one-time data import.
 */
object LegacyDataMigrator {
    private const val PREFS = "waleed_room_migration"
    private const val KEY_DONE = "phase23_sqlite_to_room_done_v1"

    suspend fun migrateIfNeeded(context: Context, db: WaleedRoomDatabase) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return@withContext

        val legacyPath = appContext.getDatabasePath(DatabaseHelper.DATABASE_NAME)
        if (!legacyPath.exists()) {
            seedFreshRoomDatabase(db)
            prefs.edit().putBoolean(KEY_DONE, true).apply()
            return@withContext
        }

        val legacy = DatabaseHelper(appContext).readableDatabase
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
        legacy.close()
        prefs.edit().putBoolean(KEY_DONE, true).apply()
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
