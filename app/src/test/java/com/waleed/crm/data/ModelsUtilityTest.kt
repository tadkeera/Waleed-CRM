package com.waleed.crm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsUtilityTest {
    @Test
    fun yemenPhoneCode_isAddedToLocalNumbers() {
        assertEquals("+967777123456", "0777123456".withYemenPhoneCode())
        assertEquals("+967777123456", "777123456".withYemenPhoneCode())
        assertEquals("+967777123456", "967777123456".withYemenPhoneCode())
        assertEquals("+967777123456", "00967777123456".withYemenPhoneCode())
    }

    @Test
    fun doctorPrefix_isNormalizedWithoutDuplication() {
        assertEquals("وليد", "د. وليد".stripDoctorPrefix())
        assertEquals("د. وليد", "دكتور وليد".withDoctorPrefix())
        assertEquals("د. وليد", "د. وليد".withDoctorPrefix())
    }

    @Test
    fun arabicSearchKey_normalizesCommonArabicDifferences() {
        assertEquals("احمد", "أحمد".normalizedArabicSearchKey())
        assertEquals("فاطمه", "فاطمة".normalizedArabicSearchKey())
        assertEquals("علي", "على".normalizedArabicSearchKey())
    }

    @Test
    fun clientNormalization_savesDoctorNameWithoutPrefix() {
        val client = Client(name = "د. وليد", phone = "777123456", secondPhone = "+967")
            .normalizedForSaving()
        assertEquals("وليد", client.name)
        assertEquals("+967777123456", client.phone)
        assertEquals("", client.secondPhone)
    }
}
