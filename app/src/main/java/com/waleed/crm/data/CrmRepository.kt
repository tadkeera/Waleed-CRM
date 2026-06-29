package com.waleed.crm.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class DashboardAnalytics(
    val totalClassifiedDoctors: Int,
    val unclassifiedDoctors: List<Client>,
    val contactedDoctorsThisWeek: List<DoctorMessageCount>,
    val uncontactedDoctorsThisWeek: List<Client>
) {
    companion object { val Empty = DashboardAnalytics(0, emptyList(), emptyList(), emptyList()) }
}

class CrmRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    private val colorsPool = listOf(
        "#E57373", "#81C784", "#64B5F6", "#BA68C8",
        "#FFB74D", "#4DD0E1", "#F06292", "#AED581",
        "#FF8A65", "#9575CD", "#4DB6AC", "#DCE775"
    )

    suspend fun getAllClients(): List<Client> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Client>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_CLIENTS, null, null, null, null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToClient(it))
            }
        }
        list
    }

    suspend fun getClientById(id: Long): Client? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_CLIENTS, null, "id = ?", arrayOf(id.toString()), null, null, null)
        cursor.use {
            if (it.moveToFirst()) cursorToClient(it) else null
        }
    }

    suspend fun getClientByPhone(phone: String): Client? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_CLIENTS, null, "phone LIKE ? OR second_phone LIKE ?", arrayOf("%$phone%", "%$phone%"), null, null, null)
        cursor.use {
            if (it.moveToFirst()) cursorToClient(it) else null
        }
    }

    suspend fun insertClient(client: Client): Long = withContext(Dispatchers.IO) {
        val normalizedClient = client.normalizedForSaving()
        val db = dbHelper.writableDatabase
        var cardColor = normalizedClient.cardColor
        if (normalizedClient.clientType == "طبيب" && normalizedClient.specialization.isNotBlank()) {
            val existingSpec = getSpecializationByName(normalizedClient.specialization)
            if (existingSpec != null) {
                cardColor = existingSpec.color
            } else {
                cardColor = colorsPool.random()
                addSpecializationInternal(normalizedClient.specialization, cardColor)
            }
        }
        if (normalizedClient.location.isNotBlank()) {
            addLocationInternal(normalizedClient.location)
        }

        val cv = ContentValues().apply {
            put("name", normalizedClient.name)
            put("phone", normalizedClient.phone)
            put("second_phone", normalizedClient.secondPhone)
            put("client_type", normalizedClient.clientType)
            put("specialization", normalizedClient.specialization)
            put("client_class", normalizedClient.clientClass)
            put("location", normalizedClient.location)
            put("is_classified", if (normalizedClient.isClassified) 1 else 0)
            put("card_color", cardColor)
            put("date_added", normalizedClient.dateAdded)
            put("updated_at", normalizedClient.updatedAt)
            put("notes", normalizedClient.notes)
        }
        db.insert(DatabaseHelper.TABLE_CLIENTS, null, cv)
    }

    suspend fun updateClient(client: Client): Int = withContext(Dispatchers.IO) {
        val normalizedClient = client.normalizedForSaving()
        val db = dbHelper.writableDatabase
        var cardColor = normalizedClient.cardColor
        if (normalizedClient.clientType == "طبيب" && normalizedClient.specialization.isNotBlank()) {
            val existingSpec = getSpecializationByName(normalizedClient.specialization)
            if (existingSpec != null) {
                cardColor = existingSpec.color
            } else {
                cardColor = colorsPool.random()
                addSpecializationInternal(normalizedClient.specialization, cardColor)
            }
        }
        if (normalizedClient.location.isNotBlank()) {
            addLocationInternal(normalizedClient.location)
        }

        val cv = ContentValues().apply {
            put("name", normalizedClient.name)
            put("phone", normalizedClient.phone)
            put("second_phone", normalizedClient.secondPhone)
            put("client_type", normalizedClient.clientType)
            put("specialization", normalizedClient.specialization)
            put("client_class", normalizedClient.clientClass)
            put("location", normalizedClient.location)
            put("is_classified", if (normalizedClient.isClassified) 1 else 0)
            put("card_color", cardColor)
            put("date_added", normalizedClient.dateAdded)
            put("updated_at", normalizedClient.updatedAt)
            put("notes", normalizedClient.notes)
        }
        db.update(DatabaseHelper.TABLE_CLIENTS, cv, "id = ?", arrayOf(client.id.toString()))
    }

    suspend fun deleteClient(id: Long): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_CLIENTS, "id = ?", arrayOf(id.toString()))
    }

    suspend fun getSpecializations(): List<Specialization> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Specialization>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_SPECIALIZATIONS, null, null, null, null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Specialization(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        color = it.getString(it.getColumnIndexOrThrow("color"))
                    )
                )
            }
        }
        list
    }

    private fun getSpecializationByName(name: String): Specialization? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_SPECIALIZATIONS, null, "name = ?", arrayOf(name), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) {
                Specialization(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    color = it.getString(it.getColumnIndexOrThrow("color"))
                )
            } else null
        }
    }

    private fun addSpecializationInternal(name: String, color: String): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
            put("color", color)
        }
        return db.insertWithOnConflict(DatabaseHelper.TABLE_SPECIALIZATIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    suspend fun getLocations(): List<Location> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Location>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_LOCATIONS, null, null, null, null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Location(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name"))
                    )
                )
            }
        }
        list
    }

    private fun addLocationInternal(name: String): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply { put("name", name) }
        return db.insertWithOnConflict(DatabaseHelper.TABLE_LOCATIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    suspend fun getPharmaciesByClientId(clientId: Long): List<Pharmacy> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Pharmacy>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_PHARMACIES, null, "client_id = ?", arrayOf(clientId.toString()), null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Pharmacy(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        clientId = it.getLong(it.getColumnIndexOrThrow("client_id"))
                    )
                )
            }
        }
        list
    }

    suspend fun addPharmacy(pharmacy: Pharmacy): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", pharmacy.name)
            put("client_id", pharmacy.clientId)
        }
        db.insert(DatabaseHelper.TABLE_PHARMACIES, null, cv)
    }

    suspend fun getAllGalleryFiles(): List<GalleryFile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<GalleryFile>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_GALLERY_FILES, null, null, null, null, null, "date_added DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    GalleryFile(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        filePath = it.getString(it.getColumnIndexOrThrow("file_path")),
                        type = it.getString(it.getColumnIndexOrThrow("type")),
                        dateAdded = it.getLong(it.getColumnIndexOrThrow("date_added"))
                    )
                )
            }
        }
        list
    }

    suspend fun insertGalleryFile(file: GalleryFile): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", file.name)
            put("file_path", file.filePath)
            put("type", file.type)
            put("date_added", file.dateAdded)
        }
        db.insert(DatabaseHelper.TABLE_GALLERY_FILES, null, cv)
    }

    suspend fun deleteGalleryFile(id: Long): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_GALLERY_FILES, "id = ?", arrayOf(id.toString()))
    }

    suspend fun findDuplicateClient(client: Client): Client? = withContext(Dispatchers.IO) {
        val n = client.normalizedForSaving()
        val args = mutableListOf<String>()
        val conditions = mutableListOf<String>()
        if (n.phone.isNotBlank() && n.phone != "+967") { conditions.add("phone = ? OR second_phone = ?"); args.add(n.phone); args.add(n.phone) }
        if (n.secondPhone.isNotBlank()) { conditions.add("phone = ? OR second_phone = ?"); args.add(n.secondPhone); args.add(n.secondPhone) }
        if (n.name.isNotBlank()) { conditions.add("client_type = ? AND name = ?"); args.add(n.clientType); args.add(n.name) }
        if (conditions.isEmpty()) return@withContext null
        val where = "(${conditions.joinToString(") OR (")}) AND id != ?"
        args.add(n.id.toString())
        dbHelper.readableDatabase.query(DatabaseHelper.TABLE_CLIENTS, null, where, args.toTypedArray(), null, null, "id DESC", "1").use { if (it.moveToFirst()) cursorToClient(it) else null }
    }

    suspend fun insertMessageTemplate(template: MessageTemplate): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("title", template.title.trim()); put("body", template.body.trim()); put("date_added", template.dateAdded) }
        dbHelper.writableDatabase.insert(DatabaseHelper.TABLE_MESSAGE_TEMPLATES, null, cv)
    }

    suspend fun deleteMessageTemplate(id: Long): Int = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DatabaseHelper.TABLE_MESSAGE_TEMPLATES, "id = ?", arrayOf(id.toString()))
    }

    suspend fun getMessageTemplates(): List<MessageTemplate> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MessageTemplate>()
        dbHelper.readableDatabase.query(DatabaseHelper.TABLE_MESSAGE_TEMPLATES, null, null, null, null, null, "date_added DESC").use {
            while (it.moveToNext()) list.add(MessageTemplate(it.getLong(it.getColumnIndexOrThrow("id")), it.getString(it.getColumnIndexOrThrow("title")) ?: "", it.getString(it.getColumnIndexOrThrow("body")) ?: "", it.getLong(it.getColumnIndexOrThrow("date_added"))))
        }
        list
    }

    suspend fun createMessageCampaign(c: MessageCampaign): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("title", c.title); put("target_count", c.targetCount); put("sent_count", c.sentCount); put("message_mode", c.messageMode); put("attachment_name", c.attachmentName); put("date_created", c.dateCreated) }
        dbHelper.writableDatabase.insert(DatabaseHelper.TABLE_MESSAGE_CAMPAIGNS, null, cv)
    }

    suspend fun updateCampaignSentCount(campaignId: Long, sentCount: Int): Int = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("sent_count", sentCount) }
        dbHelper.writableDatabase.update(DatabaseHelper.TABLE_MESSAGE_CAMPAIGNS, cv, "id = ?", arrayOf(campaignId.toString()))
    }

    suspend fun getMessageCampaigns(): List<MessageCampaign> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MessageCampaign>()
        dbHelper.readableDatabase.query(DatabaseHelper.TABLE_MESSAGE_CAMPAIGNS, null, null, null, null, null, "date_created DESC").use {
            while (it.moveToNext()) list.add(MessageCampaign(
                id = it.getLong(it.getColumnIndexOrThrow("id")), title = it.getString(it.getColumnIndexOrThrow("title")) ?: "", targetCount = it.getInt(it.getColumnIndexOrThrow("target_count")), sentCount = it.getInt(it.getColumnIndexOrThrow("sent_count")), messageMode = it.getString(it.getColumnIndexOrThrow("message_mode")) ?: "TEXT_ONLY", attachmentName = it.getString(it.getColumnIndexOrThrow("attachment_name")) ?: "", dateCreated = it.getLong(it.getColumnIndexOrThrow("date_created"))))
        }
        list
    }

    suspend fun logMessage(log: MessageLog) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("client_id", log.clientId); put("timestamp", log.timestamp); put("message_text", log.messageText); put("attachment_name", log.attachmentName); put("attachment_type", log.attachmentType); put("send_mode", log.sendMode); put("campaign_id", log.campaignId); put("status", log.status) }
        dbHelper.writableDatabase.insert(DatabaseHelper.TABLE_MESSAGE_LOGS, null, cv)
    }

    suspend fun logMessage(clientId: Long) = logMessage(MessageLog(clientId = clientId))

    suspend fun getMessageLogsByClientId(clientId: Long): List<MessageLog> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MessageLog>()
        dbHelper.readableDatabase.query(DatabaseHelper.TABLE_MESSAGE_LOGS, null, "client_id = ?", arrayOf(clientId.toString()), null, null, "timestamp DESC").use { while (it.moveToNext()) list.add(cursorToMessageLog(it)) }
        list
    }

    suspend fun getDashboardAnalytics(): DashboardAnalytics = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        
        var totalClassifiedDoctors = 0
        db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_CLIENTS} WHERE client_type = 'طبيب' AND is_classified = 1 AND specialization != '' AND location != ''", null).use {
            if (it.moveToFirst()) totalClassifiedDoctors = it.getInt(0)
        }

        val unclassifiedDoctors = mutableListOf<Client>()
        db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_CLIENTS} WHERE client_type = 'طبيب' AND (is_classified = 0 OR specialization = '' OR location = '')", null).use {
            while (it.moveToNext()) {
                unclassifiedDoctors.add(cursorToClient(it))
            }
        }

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val startOfWeek = cal.timeInMillis

        val contactedDoctorsThisWeek = mutableListOf<DoctorMessageCount>()
        val contactedQuery = """
            SELECT c.*, COUNT(m.id) as msg_count 
            FROM ${DatabaseHelper.TABLE_CLIENTS} c 
            INNER JOIN ${DatabaseHelper.TABLE_MESSAGE_LOGS} m ON c.id = m.client_id 
            WHERE c.client_type = 'طبيب' AND m.timestamp >= ? 
            GROUP BY c.id
        """.trimIndent()
        db.rawQuery(contactedQuery, arrayOf(startOfWeek.toString())).use {
            while (it.moveToNext()) {
                val client = cursorToClient(it)
                val count = it.getInt(it.getColumnIndexOrThrow("msg_count"))
                contactedDoctorsThisWeek.add(DoctorMessageCount(client, count))
            }
        }

        val uncontactedDoctorsThisWeek = mutableListOf<Client>()
        val uncontactedQuery = """
            SELECT * FROM ${DatabaseHelper.TABLE_CLIENTS} WHERE client_type = 'طبيب' AND id NOT IN (
                SELECT DISTINCT client_id FROM ${DatabaseHelper.TABLE_MESSAGE_LOGS} WHERE timestamp >= ?
            )
        """.trimIndent()
        db.rawQuery(uncontactedQuery, arrayOf(startOfWeek.toString())).use {
            while (it.moveToNext()) {
                uncontactedDoctorsThisWeek.add(cursorToClient(it))
            }
        }

        DashboardAnalytics(
            totalClassifiedDoctors = totalClassifiedDoctors,
            unclassifiedDoctors = unclassifiedDoctors,
            contactedDoctorsThisWeek = contactedDoctorsThisWeek,
            uncontactedDoctorsThisWeek = uncontactedDoctorsThisWeek
        )
    }

    private fun cursorToClient(cursor: Cursor): Client {
        val clientType = cursor.getString(cursor.getColumnIndexOrThrow("client_type")) ?: "طبيب"
        val rawName = cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: ""
        val rawPhone = cursor.getString(cursor.getColumnIndexOrThrow("phone")) ?: ""
        val rawSecondPhone = cursor.getString(cursor.getColumnIndexOrThrow("second_phone")) ?: ""
        return Client(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            name = if (clientType == "طبيب") rawName.withDoctorPrefix() else rawName.trim(),
            phone = rawPhone.withYemenPhoneCode(),
            secondPhone = if (rawSecondPhone.isBlank()) "" else rawSecondPhone.withYemenPhoneCode(),
            clientType = clientType,
            specialization = cursor.getString(cursor.getColumnIndexOrThrow("specialization")) ?: "",
            clientClass = cursor.getString(cursor.getColumnIndexOrThrow("client_class")) ?: "B",
            location = cursor.getString(cursor.getColumnIndexOrThrow("location")) ?: "",
            isClassified = cursor.getInt(cursor.getColumnIndexOrThrow("is_classified")) == 1,
            cardColor = cursor.getString(cursor.getColumnIndexOrThrow("card_color")) ?: "#2196F3",
            dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow("date_added")),
            updatedAt = getLongOrDefault(cursor, "updated_at", cursor.getLong(cursor.getColumnIndexOrThrow("date_added"))),
            notes = getStringOrDefault(cursor, "notes", "")
        )
    }

    private fun cursorToMessageLog(cursor: Cursor): MessageLog = MessageLog(
        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
        clientId = cursor.getLong(cursor.getColumnIndexOrThrow("client_id")),
        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
        messageText = getStringOrDefault(cursor, "message_text", ""),
        attachmentName = getStringOrDefault(cursor, "attachment_name", ""),
        attachmentType = getStringOrDefault(cursor, "attachment_type", ""),
        sendMode = getStringOrDefault(cursor, "send_mode", "TEXT_ONLY"),
        campaignId = getLongOrDefault(cursor, "campaign_id", 0L),
        status = getStringOrDefault(cursor, "status", "OPENED")
    )

    private fun getStringOrDefault(cursor: Cursor, column: String, default: String): String {
        val index = cursor.getColumnIndex(column)
        return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else default
    }
    private fun getLongOrDefault(cursor: Cursor, column: String, default: Long): Long {
        val index = cursor.getColumnIndex(column)
        return if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else default
    }
}
