package com.waleed.crm.data.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPhase23InstrumentedTest {
    private lateinit var db: WaleedRoomDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WaleedRoomDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun clientReadWriteAndSearchUseRoomDao() = runBlocking {
        val id = db.clientDao().upsert(ClientEntity(name = "وليد", phone = "+967777000111", specialization = "قلب", location = "صنعاء"))

        val byId = db.clientDao().getById(id)
        val search = db.clientDao().searchClients("وليد", 10).first()

        assertNotNull(byId)
        assertEquals("وليد", byId?.name)
        assertEquals(1, search.size)
    }

    @Test
    fun phase23RoomCoversLegacyTables() = runBlocking {
        val clientId = db.clientDao().upsert(ClientEntity(name = "اختبار", phone = "+967700000000"))
        db.catalogDao().insertSpecialization(SpecializationEntity(name = "باطنية", color = "#64B5F6"))
        db.catalogDao().insertLocation(LocationEntity(name = "عدن"))
        db.catalogDao().upsertPharmacy(PharmacyEntity(name = "صيدلية", clientId = clientId))
        db.galleryDao().upsert(GalleryFileEntity(name = "ملف", filePath = "/tmp/a.pdf", type = "PDF"))
        db.messageDao().upsertTemplate(MessageTemplateEntity(title = "قالب", body = "نص"))
        db.messageDao().upsertCampaign(MessageCampaignEntity(title = "حملة", targetCount = 1))
        db.messageDao().insertLog(MessageLogEntity(clientId = clientId, messageText = "مرحبا"))
        db.followUpDao().upsert(FollowUpEntity(clientId = clientId, title = "متابعة", dueAt = System.currentTimeMillis()))
        db.userDao().insert(UserEntity(name = "مدير", username = "admin", passwordHash = "hash"))
        db.auditDao().insert(AuditLogEntity(action = "TEST"))
        db.segmentDao().upsert(SavedSegmentEntity(name = "شريحة"))

        assertEquals(1, db.catalogDao().getSpecializations().size)
        assertEquals(1, db.catalogDao().getLocations().size)
        assertEquals(1, db.catalogDao().getPharmaciesByClientId(clientId).size)
        assertEquals(1, db.galleryDao().getAll().size)
        assertEquals(1, db.messageDao().getTemplates().size)
        assertEquals(1, db.messageDao().getCampaigns().size)
        assertEquals(1, db.messageDao().getLogsForClient(clientId).size)
        assertEquals(1, db.followUpDao().getPendingFollowUps().size)
        assertEquals(1, db.userDao().getUsers().size)
        assertTrue(db.auditDao().getAudit().isNotEmpty())
        assertEquals(1, db.segmentDao().getSegments().size)
    }
}
