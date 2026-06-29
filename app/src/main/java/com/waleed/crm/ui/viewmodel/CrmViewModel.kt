package com.waleed.crm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waleed.crm.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CrmViewModel(private val repository: CrmRepository) : ViewModel() {

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients

    private val _specializations = MutableStateFlow<List<Specialization>>(emptyList())
    val specializations: StateFlow<List<Specialization>> = _specializations

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations

    private val _nearbyPharmacies = MutableStateFlow<List<Pharmacy>>(emptyList())
    val nearbyPharmacies: StateFlow<List<Pharmacy>> = _nearbyPharmacies

    private val _galleryFiles = MutableStateFlow<List<GalleryFile>>(emptyList())
    val galleryFiles: StateFlow<List<GalleryFile>> = _galleryFiles

    private val _dashboardAnalytics = MutableStateFlow(DashboardAnalytics.Empty)
    val dashboardAnalytics: StateFlow<DashboardAnalytics> = _dashboardAnalytics

    private val _messageTemplates = MutableStateFlow<List<MessageTemplate>>(emptyList())
    val messageTemplates: StateFlow<List<MessageTemplate>> = _messageTemplates

    private val _messageCampaigns = MutableStateFlow<List<MessageCampaign>>(emptyList())
    val messageCampaigns: StateFlow<List<MessageCampaign>> = _messageCampaigns

    // State for Bulk WhatsApp Messaging Selection
    var selectedDoctorIdsForBulk = mutableListOf<Long>()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _clients.value = repository.getAllClients()
            _specializations.value = repository.getSpecializations()
            _locations.value = repository.getLocations()
            _galleryFiles.value = repository.getAllGalleryFiles()
            _messageTemplates.value = repository.getMessageTemplates()
            _messageCampaigns.value = repository.getMessageCampaigns()
            _dashboardAnalytics.value = repository.getDashboardAnalytics()
        }
    }

    fun getClientById(id: Long, onResult: (Client?) -> Unit) {
        viewModelScope.launch {
            val client = repository.getClientById(id)
            onResult(client)
        }
    }

    fun saveClient(client: Client, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val duplicate = repository.findDuplicateClient(client)
            if (duplicate != null) { onComplete(-duplicate.id); return@launch }
            val newId = if (client.id == 0L) {
                repository.insertClient(client)
            } else {
                repository.updateClient(client)
                client.id
            }
            loadInitialData()
            onComplete(newId)
        }
    }

    fun deleteClient(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteClient(id)
            loadInitialData()
            onComplete()
        }
    }

    fun loadNearbyPharmacies(clientId: Long) {
        if (clientId == 0L) {
            _nearbyPharmacies.value = emptyList()
            return
        }
        viewModelScope.launch {
            _nearbyPharmacies.value = repository.getPharmaciesByClientId(clientId)
        }
    }

    fun addPharmacy(name: String, clientId: Long) {
        if (name.isBlank() || clientId == 0L) return
        viewModelScope.launch {
            repository.addPharmacy(Pharmacy(name = name, clientId = clientId))
            loadNearbyPharmacies(clientId)
        }
    }

    fun addGalleryFile(name: String, filePath: String, type: String) {
        viewModelScope.launch {
            repository.insertGalleryFile(GalleryFile(name = name, filePath = filePath, type = type))
            _galleryFiles.value = repository.getAllGalleryFiles()
        }
    }

    fun deleteGalleryFile(id: Long) {
        viewModelScope.launch {
            repository.deleteGalleryFile(id)
            _galleryFiles.value = repository.getAllGalleryFiles()
        }
    }

    fun importContacts(contacts: List<Pair<String, String>>) {
        viewModelScope.launch {
            for ((name, phone) in contacts) {
                val existing = repository.getClientByPhone(phone)
                if (existing == null) {
                    val client = Client(
                        name = name,
                        phone = phone,
                        clientType = "طبيب",
                        isClassified = false
                    )
                    repository.insertClient(client)
                }
            }
            loadInitialData()
        }
    }

    fun addMessageTemplate(title: String, body: String) {
        if (title.isBlank() || body.isBlank()) return
        viewModelScope.launch { repository.insertMessageTemplate(MessageTemplate(title = title, body = body)); _messageTemplates.value = repository.getMessageTemplates() }
    }
    fun deleteMessageTemplate(id: Long) { viewModelScope.launch { repository.deleteMessageTemplate(id); _messageTemplates.value = repository.getMessageTemplates() } }
    fun refreshMessagingData() { viewModelScope.launch { _messageTemplates.value = repository.getMessageTemplates(); _messageCampaigns.value = repository.getMessageCampaigns() } }
    fun createMessageCampaign(campaign: MessageCampaign, onComplete: (Long) -> Unit) { viewModelScope.launch { val id = repository.createMessageCampaign(campaign); _messageCampaigns.value = repository.getMessageCampaigns(); onComplete(id) } }
    fun updateCampaignSentCount(campaignId: Long, sentCount: Int) { if (campaignId <= 0L) return; viewModelScope.launch { repository.updateCampaignSentCount(campaignId, sentCount); _messageCampaigns.value = repository.getMessageCampaigns() } }
    fun logMessage(log: MessageLog) { viewModelScope.launch { repository.logMessage(log); _dashboardAnalytics.value = repository.getDashboardAnalytics() } }
    fun getMessageLogsByClientId(clientId: Long, onResult: (List<MessageLog>) -> Unit) { viewModelScope.launch { onResult(repository.getMessageLogsByClientId(clientId)) } }
    fun logBulkMessages(clientIds: List<Long>) {
        viewModelScope.launch {
            for (id in clientIds) repository.logMessage(id)
            _dashboardAnalytics.value = repository.getDashboardAnalytics()
        }
    }
}
