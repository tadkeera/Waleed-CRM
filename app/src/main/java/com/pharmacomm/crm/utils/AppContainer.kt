package com.pharmacomm.crm.utils

import com.pharmacomm.crm.data.local.AppDatabase
import com.pharmacomm.crm.data.repository.ClientRepositoryImpl
import com.pharmacomm.crm.data.repository.ContactLogRepositoryImpl
import com.pharmacomm.crm.data.repository.LookupRepositoryImpl
import com.pharmacomm.crm.domain.repository.ClientRepository
import com.pharmacomm.crm.domain.repository.ContactLogRepository
import com.pharmacomm.crm.domain.repository.LookupRepository

object AppContainer {
    lateinit var clientRepository: ClientRepository
        private set
    lateinit var lookupRepository: LookupRepository
        private set
    lateinit var contactLogRepository: ContactLogRepository
        private set

    fun init(database: AppDatabase) {
        clientRepository = ClientRepositoryImpl(
            database.clientDao(),
            database.phoneNumberDao()
        )
        lookupRepository = LookupRepositoryImpl(database.lookupDao())
        contactLogRepository = ContactLogRepositoryImpl(database.contactLogDao())
    }
}