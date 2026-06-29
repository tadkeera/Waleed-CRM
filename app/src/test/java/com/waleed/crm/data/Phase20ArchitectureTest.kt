package com.waleed.crm.data

import com.waleed.crm.data.room.ClientEntity
import com.waleed.crm.data.room.toEntity
import com.waleed.crm.data.room.toModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase20ArchitectureTest {
    @Test fun clientEntityMappingKeepsImportantFields() {
        val client = Client(name = "د. أحمد", phone = "771234567", specialization = "قلب", location = "صنعاء", notes = "مهم")
        val entity = client.toEntity()
        assertEquals("أحمد", entity.name)
        assertEquals("قلب", entity.specialization)
        assertEquals("صنعاء", entity.location)
    }

    @Test fun clientEntityToModelAddsDoctorPrefix() {
        val entity = ClientEntity(name = "وليد", phone = "+967777777777", clientType = "طبيب")
        val model = entity.toModel()
        assertTrue(model.name.startsWith("د."))
    }

    @Test fun savedSegmentDefaultsAreSafe() {
        val segment = SavedSegment(name = "أطباء القلب")
        assertEquals("الكل", segment.clientType)
        assertEquals(false, segment.onlyOverdueFollowUp)
    }
}
