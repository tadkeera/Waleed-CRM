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
)

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

    suspend fun logMessage(clientId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("client_id", clientId)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert(DatabaseHelper.TABLE_MESSAGE_LOGS, null, cv)
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
            dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow("date_added"))
        )
    }
}
