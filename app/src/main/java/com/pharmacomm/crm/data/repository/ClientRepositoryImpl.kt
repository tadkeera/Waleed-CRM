package com.pharmacomm.crm.data.repository

import com.pharmacomm.crm.data.local.ClientDao
import com.pharmacomm.crm.data.local.PhoneNumberDao
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.PhoneNumber
import com.pharmacomm.crm.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ClientRepositoryImpl @Inject constructor(
    private val clientDao: ClientDao,
    private val phoneNumberDao: PhoneNumberDao
) : ClientRepository {

    override fun getAllClients(): Flow<List<Client>> = clientDao.getAllClients()

    override suspend fun getClientById(id: Long): Client? = clientDao.getClientById(id)

    override fun getClientsByType(type: String): Flow<List<Client>> = clientDao.getClientsByType(type)

    override fun searchClients(query: String): Flow<List<Client>> = clientDao.searchClients(query)

    override suspend fun insertClient(client: Client): Long {
        return clientDao.insertClient(client)
    }

    override suspend fun updateClient(client: Client) {
        clientDao.updateClient(client)
    }

    override suspend fun deleteClient(client: Client) {
        phoneNumberDao.deleteByClientId(client.id)
        clientDao.deleteClient(client)
    }

    override suspend fun updateContactInfo(clientId: Long) {
        clientDao.updateContactInfo(clientId)
    }

    override fun getTotalClientsCount(): Flow<Int> = clientDao.getTotalClientsCount()

    override fun getCompletedClientsCount(): Flow<Int> = clientDao.getCompletedClientsCount()

    override fun getIncompleteClients(): Flow<List<Client>> = clientDao.getIncompleteClients()

    override suspend fun insertPhoneNumber(phoneNumber: PhoneNumber): Long {
        return phoneNumberDao.insertPhoneNumber(phoneNumber)
    }

    override fun getPhoneNumbersByClient(clientId: Long): Flow<List<PhoneNumber>> = 
        phoneNumberDao.getPhoneNumbersByClient(clientId)

    override suspend fun getPhoneNumbersByClientSync(clientId: Long): List<PhoneNumber> =
        phoneNumberDao.getPhoneNumbersByClientSync(clientId)

    override suspend fun deletePhoneNumbersByClient(clientId: Long) {
        phoneNumberDao.deleteByClientId(clientId)
    }
}