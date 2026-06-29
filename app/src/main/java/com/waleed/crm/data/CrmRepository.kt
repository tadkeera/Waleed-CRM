package com.waleed.crm.data

import android.content.Context
import com.waleed.crm.data.repository.RepositoryModuleSet
import com.waleed.crm.data.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.Calendar


data class StatItem(val label: String, val count: Int)

data class DashboardAnalytics(
    val totalClassifiedDoctors: Int,
    val unclassifiedDoctors: List<Client>,
    val contactedDoctorsThisWeek: List<DoctorMessageCount>,
    val uncontactedDoctorsThisWeek: List<Client>,
    val totalDoctors: Int = 0,
    val totalClients: Int = 0,
    val totalCampaigns: Int = 0,
    val totalCampaignTargets: Int = 0,
    val totalCampaignOpened: Int = 0,
    val weeklyMessages: Int = 0,
    val monthlyMessages: Int = 0,
    val textOnlyCampaigns: Int = 0,
    val attachmentOnlyCampaigns: Int = 0,
    val textAndAttachmentCampaigns: Int = 0,
    val topContactedDoctors: List<DoctorMessageCount> = emptyList(),
    val overdueDoctors: List<Client> = emptyList(),
    val specializationStats: List<StatItem> = emptyList(),
    val locationStats: List<StatItem> = emptyList(),
    val recentCampaigns: List<MessageCampaign> = emptyList()
) { companion object { val Empty = DashboardAnalytics(0, emptyList(), emptyList(), emptyList()) } }

class CrmRepository(context: Context) {
    private val appContext = context.applicationContext
    private val roomDb = WaleedRoomDatabase.getInstance(appContext)
    val modules = RepositoryModuleSet.fromRoom(roomDb)

    private val clientDao = roomDb.clientDao()
    private val catalogDao = roomDb.catalogDao()
    private val galleryDao = roomDb.galleryDao()
    private val messageDao = roomDb.messageDao()
    private val followUpDao = roomDb.followUpDao()
    private val userDao = roomDb.userDao()
    private val auditDao = roomDb.auditDao()
    private val segmentDao = roomDb.segmentDao()
    private val dashboardDao = roomDb.dashboardDao()

    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val migration = migrationScope.async { LegacyDataMigrator.migrateIfNeeded(appContext, roomDb) }

    private val colorsPool = listOf(
        "#E57373", "#81C784", "#64B5F6", "#BA68C8",
        "#FFB74D", "#4DD0E1", "#F06292", "#AED581",
        "#FF8A65", "#9575CD", "#4DB6AC", "#DCE775"
    )

    private suspend fun ensureMigrated() { migration.await() }

    suspend fun getAllClients(): List<Client> = withContext(Dispatchers.IO) {
        ensureMigrated()
        clientDao.getAll().map { it.toModel() }
    }

    suspend fun getClientsPage(limit: Int = 50, offset: Int = 0): List<Client> = withContext(Dispatchers.IO) {
        ensureMigrated()
        clientDao.getPage(limit, offset).map { it.toModel() }
    }

    suspend fun getDashboardAnalyticsFast(): DashboardAnalytics = withContext(Dispatchers.IO) {
        ensureMigrated()
        DashboardAnalytics.Empty.copy(
            totalClients = dashboardDao.totalClients(),
            totalDoctors = dashboardDao.totalDoctors(),
            weeklyMessages = dashboardDao.pendingFollowUpsCount(),
            specializationStats = dashboardDao.specializationStats(5).map { StatItem(it.label, it.count) },
            locationStats = dashboardDao.locationStats(5).map { StatItem(it.label, it.count) }
        )
    }

    fun observeClientsPage(page: Int, pageSize: Int) = modules.clients.observePage(page, pageSize).onStart { ensureMigrated() }
    fun observeSmartSearch(query: String, limit: Int = 100) = modules.clients.search(query, limit).onStart { ensureMigrated() }

    suspend fun getClientById(id: Long): Client? = withContext(Dispatchers.IO) {
        ensureMigrated()
        clientDao.getById(id)?.toModel()
    }

    suspend fun getClientByPhone(phone: String): Client? = withContext(Dispatchers.IO) {
        ensureMigrated()
        clientDao.getByPhone(phone)?.toModel()
    }

    suspend fun insertClient(client: Client): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        val normalizedClient = prepareClientForRoom(client)
        clientDao.upsert(normalizedClient.toEntity())
    }

    suspend fun updateClient(client: Client): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        val normalizedClient = prepareClientForRoom(client)
        clientDao.update(normalizedClient.toEntity())
    }

    private suspend fun prepareClientForRoom(client: Client): Client {
        var normalizedClient = client.normalizedForSaving()
        var cardColor = normalizedClient.cardColor
        if (normalizedClient.clientType == "طبيب" && normalizedClient.specialization.isNotBlank()) {
            val existingSpec = catalogDao.getSpecializationByName(normalizedClient.specialization)
            if (existingSpec != null) {
                cardColor = existingSpec.color
            } else {
                cardColor = colorsPool.random()
                catalogDao.insertSpecialization(SpecializationEntity(name = normalizedClient.specialization, color = cardColor))
            }
        }
        if (normalizedClient.location.isNotBlank()) {
            catalogDao.insertLocation(LocationEntity(name = normalizedClient.location))
        }
        normalizedClient = normalizedClient.copy(cardColor = cardColor)
        return normalizedClient
    }

    suspend fun deleteClient(id: Long): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        clientDao.deleteById(id)
    }

    suspend fun getSpecializations(): List<Specialization> = withContext(Dispatchers.IO) {
        ensureMigrated()
        catalogDao.getSpecializations().map { it.toModel() }
    }

    suspend fun getLocations(): List<Location> = withContext(Dispatchers.IO) {
        ensureMigrated()
        catalogDao.getLocations().map { it.toModel() }
    }

    suspend fun getPharmaciesByClientId(clientId: Long): List<Pharmacy> = withContext(Dispatchers.IO) {
        ensureMigrated()
        catalogDao.getPharmaciesByClientId(clientId).map { it.toModel() }
    }

    suspend fun addPharmacy(pharmacy: Pharmacy): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        catalogDao.upsertPharmacy(pharmacy.toEntity())
    }

    suspend fun getAllGalleryFiles(): List<GalleryFile> = withContext(Dispatchers.IO) {
        ensureMigrated()
        galleryDao.getAll().map { it.toModel() }
    }

    suspend fun insertGalleryFile(file: GalleryFile): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        galleryDao.upsert(file.toEntity())
    }

    suspend fun deleteGalleryFile(id: Long): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        galleryDao.deleteById(id)
    }

    suspend fun findDuplicateClient(client: Client): Client? = withContext(Dispatchers.IO) {
        ensureMigrated()
        val n = client.normalizedForSaving()
        clientDao.getAll().map { it.toModel() }.firstOrNull { existing ->
            existing.id != n.id && (
                (n.phone.isNotBlank() && n.phone != "+967" && (existing.phone == n.phone || existing.secondPhone == n.phone)) ||
                    (n.secondPhone.isNotBlank() && (existing.phone == n.secondPhone || existing.secondPhone == n.secondPhone)) ||
                    (n.name.isNotBlank() && existing.clientType == n.clientType && existing.name.stripDoctorPrefix() == n.name.stripDoctorPrefix())
                )
        }
    }

    suspend fun insertMessageTemplate(template: MessageTemplate): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.upsertTemplate(template.copy(title = template.title.trim(), body = template.body.trim()).toEntity())
    }

    suspend fun deleteMessageTemplate(id: Long): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.deleteTemplate(id)
    }

    suspend fun getMessageTemplates(): List<MessageTemplate> = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.getTemplates().map { it.toModel() }
    }

    suspend fun createMessageCampaign(c: MessageCampaign): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.upsertCampaign(c.toEntity())
    }

    suspend fun updateCampaignSentCount(campaignId: Long, sentCount: Int): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.updateCampaignSentCount(campaignId, sentCount)
    }

    suspend fun getMessageCampaigns(): List<MessageCampaign> = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.getCampaigns().map { it.toModel() }
    }

    suspend fun logMessage(log: MessageLog): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.insertLog(log.toEntity())
    }

    suspend fun logMessage(clientId: Long) = logMessage(MessageLog(clientId = clientId))

    suspend fun getMessageLogsByClientId(clientId: Long): List<MessageLog> = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.getLogsForClient(clientId).map { it.toModel() }
    }

    suspend fun getAllMessageLogs(): List<MessageLog> = withContext(Dispatchers.IO) {
        ensureMigrated()
        messageDao.getAllLogs().map { it.toModel() }
    }

    suspend fun insertFollowUp(followUp: FollowUp): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        val entity = followUp.copy(
            title = followUp.title.trim(),
            notes = followUp.notes.trim(),
            status = if (followUp.status.isBlank()) "PENDING" else followUp.status
        ).toEntity()
        followUpDao.upsert(entity)
    }

    suspend fun updateFollowUpStatus(id: Long, status: String): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        followUpDao.updateStatus(id, status)
    }

    suspend fun deleteFollowUp(id: Long): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        followUpDao.deleteById(id)
    }

    suspend fun getFollowUpsByClientId(clientId: Long): List<FollowUp> = withContext(Dispatchers.IO) {
        ensureMigrated()
        followUpDao.getByClientId(clientId).map { it.toModel() }
    }

    suspend fun getPendingFollowUps(): List<FollowUpWithClient> = withContext(Dispatchers.IO) {
        ensureMigrated()
        followUpDao.getPendingFollowUps().map { followUpEntity ->
            val followUp = followUpEntity.toModel()
            FollowUpWithClient(followUp, clientDao.getById(followUp.clientId)?.toModel())
        }
    }

    suspend fun restoreClients(clients: List<Client>, replaceExisting: Boolean): Pair<Int, Int> = withContext(Dispatchers.IO) {
        ensureMigrated()
        if (replaceExisting) clientDao.deleteAll()
        var inserted = 0
        var skipped = 0
        for (client in clients) {
            val normalized = client.normalizedForSaving().copy(id = 0, isClassified = true)
            val duplicate = if (replaceExisting) null else findDuplicateClient(normalized)
            if (duplicate != null) skipped++ else { insertClient(normalized); inserted++ }
        }
        inserted to skipped
    }

    fun exportReportsBundle(clients: List<Client>, logs: List<MessageLog>, campaigns: List<MessageCampaign>, followUps: List<FollowUpWithClient>): String = buildString {
        appendLine("Waleed CRM Reports")
        appendLine("==================")
        appendLine("إجمالي العملاء/الأطباء: ${clients.size}")
        appendLine("إجمالي الرسائل: ${logs.size}")
        appendLine("إجمالي الحملات: ${campaigns.size}")
        appendLine("المتابعات المعلقة: ${followUps.size}")
    }

    suspend fun addAuditLog(log: AuditLog): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        auditDao.insert(log.toEntity())
    }

    suspend fun getAuditLogs(limit: Int = 250): List<AuditLog> = withContext(Dispatchers.IO) {
        ensureMigrated()
        auditDao.getAudit(limit).map { it.toModel() }
    }

    suspend fun getUsers(): List<UserAccount> = withContext(Dispatchers.IO) {
        ensureMigrated()
        userDao.getUsers().map { it.toModel() }
    }

    suspend fun addUser(user: UserAccount): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        userDao.insert(user.copy(name = user.name.trim(), username = user.username.trim()).toEntity())
    }

    suspend fun updateUserRole(id: Long, role: String, active: Boolean): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        userDao.updateRole(id, role, active)
    }

    suspend fun deleteUser(id: Long): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        userDao.deleteById(id)
    }

    suspend fun saveSegment(segment: SavedSegment): Long = withContext(Dispatchers.IO) {
        ensureMigrated()
        segmentDao.upsert(segment.copy(name = segment.name.trim(), query = segment.query.trim()).toEntity())
    }

    suspend fun getSavedSegments(): List<SavedSegment> = withContext(Dispatchers.IO) {
        ensureMigrated()
        segmentDao.getSegments().map { it.toModel() }
    }

    suspend fun deleteSegment(id: Long): Int = withContext(Dispatchers.IO) {
        ensureMigrated()
        segmentDao.deleteById(id)
    }

    suspend fun smartSearch(segment: SavedSegment): List<Client> = withContext(Dispatchers.IO) {
        ensureMigrated()
        val clients = getAllClients()
        val pending = getPendingFollowUps()
        val pendingIds = pending.map { it.followUp.clientId }.toSet()
        val overdueIds = pending.filter { it.followUp.dueAt < System.currentTimeMillis() }.map { it.followUp.clientId }.toSet()
        val q = segment.query.normalizedArabicSearchKey()
        clients.filter { c ->
            val haystack = listOf(c.name, c.phone, c.secondPhone, c.specialization, c.location, c.notes).joinToString(" ").normalizedArabicSearchKey()
            (q.isBlank() || haystack.contains(q)) &&
                (segment.clientType == "الكل" || c.clientType == segment.clientType) &&
                (segment.specialization == "الكل" || c.specialization == segment.specialization) &&
                (segment.location == "الكل" || c.location == segment.location) &&
                (segment.clientClass == "الكل" || c.clientClass == segment.clientClass) &&
                (!segment.onlyPendingFollowUp || c.id in pendingIds) &&
                (!segment.onlyOverdueFollowUp || c.id in overdueIds)
        }.take(500)
    }

    fun migrationStatus(): LegacyDataMigrator.MigrationStatus = LegacyDataMigrator.status(appContext)

    fun performanceArchitectureSummary(): List<String> = listOf(
        "تم تثبيت Room كطبقة البيانات النشطة لكل الشاشات، مع منع SQLiteOpenHelper من العمل اليومي بعد اكتمال الترحيل.",
        "تمت إضافة فحص سلامة بعد الترحيل يقارن أعداد الجداول المهمة ويسجل النتيجة في سجل التدقيق.",
        "تم نقل Migration الرسمية إلى RoomMigrations.kt لتسهيل ترحيلات الإصدارات القادمة.",
        "تم تحسين تحليلات Dashboard باستعلامات DAO مباشرة بدل تحميل كل البيانات والحساب في الذاكرة.",
        "تم إصلاح القائمة الجانبية بإضافة تمرير رأسي حتى تظهر كل الصفحات على الشاشات الصغيرة."
    )

    suspend fun getDashboardAnalytics(): DashboardAnalytics = withContext(Dispatchers.IO) {
        ensureMigrated()
        val startOfWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
        val startOfMonth = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        val campaignTotals = dashboardDao.campaignTotals()

        var textOnlyCampaigns = 0
        var attachmentOnlyCampaigns = 0
        var textAndAttachmentCampaigns = 0
        dashboardDao.campaignModeCounts().forEach { item ->
            when (item.messageMode) {
                "TEXT_ONLY" -> textOnlyCampaigns = item.count
                "ATTACHMENT_ONLY" -> attachmentOnlyCampaigns = item.count
                "TEXT_AND_ATTACHMENT" -> textAndAttachmentCampaigns = item.count
            }
        }

        DashboardAnalytics(
            totalClassifiedDoctors = dashboardDao.totalClassifiedDoctors(),
            unclassifiedDoctors = dashboardDao.unclassifiedDoctors(20).map { it.toModel() },
            contactedDoctorsThisWeek = dashboardDao.contactedDoctorsSince(startOfWeek).map { DoctorMessageCount(it.client.toModel(), it.messageCount) },
            uncontactedDoctorsThisWeek = dashboardDao.uncontactedDoctorsSince(startOfWeek, 30).map { it.toModel() },
            totalDoctors = dashboardDao.totalDoctors(),
            totalClients = dashboardDao.totalClients(),
            totalCampaigns = campaignTotals.totalCampaigns,
            totalCampaignTargets = campaignTotals.totalTargets,
            totalCampaignOpened = campaignTotals.totalOpened,
            weeklyMessages = dashboardDao.countMessagesSince(startOfWeek),
            monthlyMessages = dashboardDao.countMessagesSince(startOfMonth),
            textOnlyCampaigns = textOnlyCampaigns,
            attachmentOnlyCampaigns = attachmentOnlyCampaigns,
            textAndAttachmentCampaigns = textAndAttachmentCampaigns,
            topContactedDoctors = dashboardDao.topContactedDoctors(5).map { DoctorMessageCount(it.client.toModel(), it.messageCount) },
            overdueDoctors = dashboardDao.overdueDoctors(startOfMonth, 10).map { it.toModel() },
            specializationStats = dashboardDao.specializationStats(6).map { StatItem(it.label, it.count) },
            locationStats = dashboardDao.locationStats(6).map { StatItem(it.label, it.count) },
            recentCampaigns = dashboardDao.recentCampaigns(5).map { it.toModel() }
        )
    }

}
