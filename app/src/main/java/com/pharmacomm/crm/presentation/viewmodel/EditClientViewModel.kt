package com.pharmacomm.crm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.model.PhoneNumber
import com.pharmacomm.crm.domain.model.Specialty
import com.pharmacomm.crm.domain.model.Region
import com.pharmacomm.crm.domain.repository.ClientRepository
import com.pharmacomm.crm.domain.repository.LookupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class EditClientViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val lookupRepository: LookupRepository
) : ViewModel() {

    private val _client = MutableStateFlow<Client?>(null)
    val client: StateFlow<Client?> = _client.asStateFlow()

    private val _phoneNumbers = MutableStateFlow<List<PhoneNumber>>(emptyList())
    val phoneNumbers: StateFlow<List<PhoneNumber>> = _phoneNumbers.asStateFlow()

    val specialties: Flow<List<Specialty>> = lookupRepository.getAllSpecialties()
    val regions: Flow<List<Region>> = lookupRepository.getAllRegions()

    fun loadClient(clientId: Long) {
        viewModelScope.launch {
            val c = clientRepository.getClientById(clientId)
            _client.value = c
            if (c != null) {
                val phones = clientRepository.getPhoneNumbersByClientSync(c.id)
                _phoneNumbers.value = phones
            }
        }
    }

    fun saveClient(
        client: Client,
        phones: List<String>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val clientId = if (client.id > 0) {
                clientRepository.updateClient(client)
                client.id
            } else {
                clientRepository.insertClient(client)
            }

            // Update phones
            clientRepository.deletePhoneNumbersByClient(clientId)
            phones.forEachIndexed { index, phone ->
                clientRepository.insertPhoneNumber(
                    PhoneNumber(
                        clientId = clientId,
                        number = phone,
                        isPrimary = index == 0
                    )
                )
            }

            // Auto-save lookups if not empty
            if (!client.specialty.isNullOrBlank()) {
                lookupRepository.insertSpecialty(client.specialty)
            }
            if (!client.region.isNullOrBlank()) {
                lookupRepository.insertRegion(client.region)
            }

            onComplete()
        }
    }

    fun addSpecialty(name: String) {
        viewModelScope.launch {
            lookupRepository.insertSpecialty(name)
        }
    }

    fun addRegion(name: String) {
        viewModelScope.launch {
            lookupRepository.insertRegion(name)
        }
    }
}