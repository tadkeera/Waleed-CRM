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


    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs

    private val _users = MutableStateFlow<List<UserAccount>>(emptyList())
    val users: StateFlow<List<UserAccount>> = _users

    private val _savedSegments = MutableStateFlow<List<SavedSegment>>(emptyList())
    val savedSegments: StateFlow<List<SavedSegment>> = _savedSegments

    private val _smartSearchResults = MutableStateFlow<List<Client>>(emptyList())
    val smartSearchResults: StateFlow<List<Client>> = _smartSearchResults

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
            val auditDeferred = async { repository.getAuditLogs() }
            val usersDeferred = async { repository.getUsers() }
            val segmentsDeferred = async { repository.getSavedSegments() }

            _clients.value = clientsDeferred.await()
            _specializations.value = specsDeferred.await()
            _locations.value = locationsDeferred.await()
            _galleryFiles.value = galleryDeferred.await()
            _messageTemplates.value = templatesDeferred.await()
            _messageCampaigns.value = campaignsDeferred.await()
            _dashboardAnalytics.value = analyticsDeferred.await()
            _followUps.value = followUpsDeferred.await()
            _auditLogs.value = auditDeferred.await()
            _users.value = usersDeferred.await()
            _savedSegments.value = segmentsDeferred.await()
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
            repository.addAuditLog(AuditLog(action = "إكمال متابعة", entityType = "FOLLOW_UP", entityId = id))
            _auditLogs.value = repository.getAuditLogs()
            appContext?.let { FollowUpReminderScheduler.cancel(it, id) }
            _followUps.value = repository.getPendingFollowUps()
        }
    }

    fun deleteFollowUp(id: Long) {
        viewModelScope.launch {
            repository.deleteFollowUp(id)
            repository.addAuditLog(AuditLog(action = "حذف متابعة", entityType = "FOLLOW_UP", entityId = id))
            _auditLogs.value = repository.getAuditLogs()
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
            repository.addAuditLog(AuditLog(action = if (client.id == 0L) "إضافة عميل/طبيب" else "تعديل عميل/طبيب", entityType = "CLIENT", entityId = newId, entityName = client.name))
            _auditLogs.value = repository.getAuditLogs()
            refreshClientsAndLookups()
            onComplete(newId)
        }
    }

    fun deleteClient(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteClient(id)
            repository.addAuditLog(AuditLog(action = "حذف عميل/طبيب", entityType = "CLIENT", entityId = id))
            _auditLogs.value = repository.getAuditLogs()
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


    fun addAudit(action: String, entityType: String = "APP", entityName: String = "", details: String = "") {
        viewModelScope.launch {
            repository.addAuditLog(AuditLog(action = action, entityType = entityType, entityName = entityName, details = details))
            _auditLogs.value = repository.getAuditLogs()
        }
    }

    fun refreshAuditLogs() { viewModelScope.launch { _auditLogs.value = repository.getAuditLogs() } }

    fun addUser(name: String, username: String, role: String, onComplete: () -> Unit = {}) {
        if (name.isBlank() || username.isBlank()) return
        viewModelScope.launch {
            repository.addUser(UserAccount(name = name, username = username, passwordHash = "local-pin-managed", role = role))
            repository.addAuditLog(AuditLog(action = "إضافة مستخدم", entityType = "USER", entityName = username, details = role))
            _users.value = repository.getUsers()
            _auditLogs.value = repository.getAuditLogs()
            onComplete()
        }
    }

    fun updateUserRole(id: Long, role: String, active: Boolean) {
        viewModelScope.launch {
            repository.updateUserRole(id, role, active)
            repository.addAuditLog(AuditLog(action = "تحديث صلاحية مستخدم", entityType = "USER", entityId = id, details = "$role / $active"))
            _users.value = repository.getUsers()
            _auditLogs.value = repository.getAuditLogs()
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.deleteUser(id)
            repository.addAuditLog(AuditLog(action = "حذف مستخدم", entityType = "USER", entityId = id))
            _users.value = repository.getUsers()
            _auditLogs.value = repository.getAuditLogs()
        }
    }

    fun runSmartSearch(segment: SavedSegment) {
        viewModelScope.launch { _smartSearchResults.value = repository.smartSearch(segment) }
    }

    fun saveSegment(segment: SavedSegment, onComplete: () -> Unit = {}) {
        if (segment.name.isBlank()) return
        viewModelScope.launch {
            repository.saveSegment(segment)
            repository.addAuditLog(AuditLog(action = "حفظ قائمة بحث", entityType = "SEGMENT", entityName = segment.name))
            _savedSegments.value = repository.getSavedSegments()
            _auditLogs.value = repository.getAuditLogs()
            onComplete()
        }
    }

    fun deleteSegment(id: Long) {
        viewModelScope.launch {
            repository.deleteSegment(id)
            _savedSegments.value = repository.getSavedSegments()
        }
    }

    fun performanceSummary(onResult: (List<String>) -> Unit) { onResult(repository.performanceArchitectureSummary()) }

    fun logBulkMessages(clientIds: List<Long>) {
        viewModelScope.launch {
            for (id in clientIds) repository.logMessage(id)
            _dashboardAnalytics.value = repository.getDashboardAnalytics()
        }
    }
}
