package com.waleed.crm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waleed.crm.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import com.waleed.crm.reminders.FollowUpReminderScheduler

class CrmViewModel(private val repository: CrmRepository, private val appContext: android.content.Context? = null) : ViewModel() {

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _followUps = MutableStateFlow<List<FollowUpWithClient>>(emptyList())
    val followUps: StateFlow<List<FollowUpWithClient>> = _followUps

    private var hasLoadedInitialData = false

    // State for Bulk WhatsApp Messaging Selection
    var selectedDoctorIdsForBulk = mutableListOf<Long>()

    init {
        loadInitialData()
    }

    fun loadInitialData(force: Boolean = false) {
        if (hasLoadedInitialData && !force) return
        viewModelScope.launch {
            _isLoading.value = true
            val clientsDeferred = async { repository.getAllClients() }
            val specsDeferred = async { repository.getSpecializations() }
            val locationsDeferred = async { repository.getLocations() }
            val galleryDeferred = async { repository.getAllGalleryFiles() }
            val templatesDeferred = async { repository.getMessageTemplates() }
            val campaignsDeferred = async { repository.getMessageCampaigns() }
            val analyticsDeferred = async { repository.getDashboardAnalytics() }
            val followUpsDeferred = async { repository.getPendingFollowUps() }

            _clients.value = clientsDeferred.await()
            _specializations.value = specsDeferred.await()
            _locations.value = locationsDeferred.await()
            _galleryFiles.value = galleryDeferred.await()
            _messageTemplates.value = templatesDeferred.await()
            _messageCampaigns.value = campaignsDeferred.await()
            _dashboardAnalytics.value = analyticsDeferred.await()
            _followUps.value = followUpsDeferred.await()
            reschedulePendingReminders(_followUps.value)
            hasLoadedInitialData = true
            _isLoading.value = false
        }
    }

    fun refreshDashboardAnalytics() {
        viewModelScope.launch { _dashboardAnalytics.value = repository.getDashboardAnalytics() }
    }

    private fun refreshClientsAndLookups() {
        viewModelScope.launch {
            _clients.value = repository.getAllClients()
            _specializations.value = repository.getSpecializations()
            _locations.value = repository.getLocations()
            _dashboardAnalytics.value = repository.getDashboardAnalytics()
        }
    }

    fun refreshFollowUps() {
        viewModelScope.launch {
            _followUps.value = repository.getPendingFollowUps()
            reschedulePendingReminders(_followUps.value)
        }
    }

    fun getFollowUpsByClientId(clientId: Long, onResult: (List<FollowUp>) -> Unit) {
        viewModelScope.launch { onResult(repository.getFollowUpsByClientId(clientId)) }
    }

    fun addFollowUp(clientId: Long, title: String, dueAt: Long, notes: String = "", onComplete: () -> Unit = {}) {
        if (clientId == 0L || title.isBlank()) return
        viewModelScope.launch {
            val id = repository.insertFollowUp(FollowUp(clientId = clientId, title = title, dueAt = dueAt, notes = notes))
            _followUps.value = repository.getPendingFollowUps()
            _followUps.value.firstOrNull { it.followUp.id == id }?.let { item -> appContext?.let { FollowUpReminderScheduler.schedule(it, item) } }
            onComplete()
        }
    }

    fun completeFollowUp(id: Long) {
        viewModelScope.launch {
            repository.updateFollowUpStatus(id, "DONE")
            appContext?.let { FollowUpReminderScheduler.cancel(it, id) }
            _followUps.value = repository.getPendingFollowUps()
        }
    }

    fun deleteFollowUp(id: Long) {
        viewModelScope.launch {
            repository.deleteFollowUp(id)
            appContext?.let { FollowUpReminderScheduler.cancel(it, id) }
            _followUps.value = repository.getPendingFollowUps()
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
            refreshClientsAndLookups()
            onComplete(newId)
        }
    }

    fun deleteClient(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteClient(id)
            refreshClientsAndLookups()
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
            refreshClientsAndLookups()
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
    fun getAllMessageLogs(onResult: (List<MessageLog>) -> Unit) {
        viewModelScope.launch { onResult(repository.getAllMessageLogs()) }
    }

    fun importClientsFromCsv(csv: String, onComplete: (inserted: Int, skipped: Int) -> Unit) {
        viewModelScope.launch {
            var inserted = 0
            var skipped = 0
            val lines = csv.lines().filter { it.isNotBlank() }
            val dataLines = if (lines.firstOrNull()?.lowercase()?.contains("phone") == true) lines.drop(1) else lines
            for (line in dataLines) {
                val cols = parseCsvLine(line)
                val name = cols.getOrNull(0)?.trim().orEmpty()
                val phone = cols.getOrNull(1)?.trim().orEmpty()
                if (name.isBlank() || phone.isBlank()) { skipped++; continue }
                val client = Client(
                    name = name,
                    phone = phone,
                    clientType = cols.getOrNull(2)?.ifBlank { "طبيب" } ?: "طبيب",
                    specialization = cols.getOrNull(3).orEmpty(),
                    clientClass = cols.getOrNull(4)?.ifBlank { "B" } ?: "B",
                    location = cols.getOrNull(5).orEmpty(),
                    notes = cols.getOrNull(6).orEmpty(),
                    isClassified = true
                )
                if (repository.findDuplicateClient(client) == null) {
                    repository.insertClient(client)
                    inserted++
                } else skipped++
            }
            refreshClientsAndLookups()
            onComplete(inserted, skipped)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> { current.append('"'); i++ }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    fun restoreClientsFromBackup(clients: List<Client>, replaceExisting: Boolean, onComplete: (inserted: Int, skipped: Int) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreClients(clients, replaceExisting)
            refreshClientsAndLookups()
            onComplete(result.first, result.second)
        }
    }

    private fun reschedulePendingReminders(items: List<FollowUpWithClient>) {
        val context = appContext ?: return
        FollowUpReminderScheduler.ensureChannel(context)
        items.forEach { FollowUpReminderScheduler.schedule(context, it) }
    }

    fun logBulkMessages(clientIds: List<Long>) {
        viewModelScope.launch {
            for (id in clientIds) repository.logMessage(id)
            _dashboardAnalytics.value = repository.getDashboardAnalytics()
        }
    }
}
