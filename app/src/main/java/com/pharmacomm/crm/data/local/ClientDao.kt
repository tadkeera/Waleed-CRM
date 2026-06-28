package com.pharmacomm.crm.data.local

import androidx.room.*
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.ImportanceClass
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): Client?

    @Query("SELECT * FROM clients WHERE clientType = :type ORDER BY name ASC")
    fun getClientsByType(type: String): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE importanceClass = :importance ORDER BY name ASC")
    fun getClientsByImportance(importance: String): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE specialty LIKE '%' || :query || '%' OR region LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchClients(query: String): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("UPDATE clients SET lastContactDate = :date, contactCount = contactCount + 1 WHERE id = :clientId")
    suspend fun updateContactInfo(clientId: Long, date: Date = Date())

    @Query("SELECT COUNT(*) FROM clients")
    fun getTotalClientsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM clients WHERE specialty IS NOT NULL AND importanceClass != 'NONE'")
    fun getCompletedClientsCount(): Flow<Int>

    @Query("SELECT * FROM clients WHERE (specialty IS NULL OR importanceClass = 'NONE') ORDER BY name ASC")
    fun getIncompleteClients(): Flow<List<Client>>
}