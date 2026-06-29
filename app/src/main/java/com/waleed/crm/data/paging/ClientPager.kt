package com.waleed.crm.data.paging

import com.waleed.crm.data.Client
import com.waleed.crm.data.CrmRepository

class ClientPager(private val repository: CrmRepository, private val pageSize: Int = 50) {
    private var page = 0
    private var cached: List<Client> = emptyList()

    suspend fun reset(): List<Client> {
        page = 0
        cached = repository.getClientsPage(pageSize, 0)
        return cached
    }

    suspend fun loadNext(): List<Client> {
        page += 1
        val next = repository.getClientsPage(pageSize, page * pageSize)
        cached = cached + next
        return cached
    }
}
