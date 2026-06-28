package com.pharmacomm.crm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.usecase.GetClientsUseCase
import com.pharmacomm.crm.domain.usecase.SearchClientsUseCase
import com.pharmacomm.crm.domain.repository.ClientRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientListUiState(
    val clients: List<Client> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)

class ClientListViewModel @Inject constructor(
    private val getClientsUseCase: GetClientsUseCase,
    private val searchClientsUseCase: SearchClientsUseCase,
    private val clientRepository: ClientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientListUiState())
    val uiState: StateFlow<ClientListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeClients()
    }

    private fun observeClients() {
        viewModelScope.launch {
            combine(
                _searchQuery,
                searchClientsUseCase(_searchQuery.value)
            ) { query, clients ->
                ClientListUiState(
                    clients = clients,
                    isLoading = false,
                    searchQuery = query
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            clientRepository.deleteClient(client)
        }
    }

    fun importContacts(contacts: List<Pair<String, String>>) {
        viewModelScope.launch {
            contacts.forEach { (name, phone) ->
                // Simplified: create client with primary phone
                val client = Client(
                    name = name,
                    clientType = com.pharmacomm.crm.domain.model.ClientType.DOCTOR,
                    importanceClass = com.pharmacomm.crm.domain.model.ImportanceClass.CLASS_B
                )
                val clientId = clientRepository.insertClient(client)
                clientRepository.insertPhoneNumber(
                    com.pharmacomm.crm.domain.model.PhoneNumber(
                        clientId = clientId,
                        number = phone,
                        isPrimary = true
                    )
                )
            }
        }
    }
}