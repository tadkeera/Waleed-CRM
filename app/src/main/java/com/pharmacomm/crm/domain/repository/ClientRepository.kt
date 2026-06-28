package com.pharmacomm.crm.domain.repository

import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.PhoneNumber
import kotlinx.coroutines.flow.Flow

interface ClientRepository {
    fun getAllClients(): Flow<List<Client>>
    suspend fun getClientById(id: Long): Client?
    fun getClientsByType(type: String): Flow<List<Client>>
    fun searchClients(query: String): Flow<List<Client>>
    suspend fun insertClient(client: Client): Long
    suspend fun updateClient(client: Client)
    suspend fun deleteClient(client: Client)
    suspend fun updateContactInfo(clientId: Long)
    fun getTotalClientsCount(): Flow<Int>
    fun getCompletedClientsCount(): Flow<Int>
    fun getIncompleteClients(): Flow<List<Client>>
    
    suspend fun insertPhoneNumber(phoneNumber: PhoneNumber): Long
    fun getPhoneNumbersByClient(clientId: Long): Flow<List<PhoneNumber>>
    suspend fun getPhoneNumbersByClientSync(clientId: Long): List<PhoneNumber>
    suspend fun deletePhoneNumbersByClient(clientId: Long)
}