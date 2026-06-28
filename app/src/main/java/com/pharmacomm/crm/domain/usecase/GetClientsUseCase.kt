package com.pharmacomm.crm.domain.usecase

import com.pharmacomm.crm.domain.model.Client
import com.pharmacomm.crm.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClientsUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    operator fun invoke(): Flow<List<Client>> = repository.getAllClients()
}