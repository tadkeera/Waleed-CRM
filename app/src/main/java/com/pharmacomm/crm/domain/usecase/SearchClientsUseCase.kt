package com.pharmacomm.crm.domain.usecase

import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchClientsUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    operator fun invoke(query: String): Flow<List<Client>> {
        return if (query.isBlank()) repository.getAllClients()
        else repository.searchClients(query)
    }
}