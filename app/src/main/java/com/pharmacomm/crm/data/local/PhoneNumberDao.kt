package com.pharmacomm.crm.data.local

import androidx.room.*
import com.pharmacomm.crm.domain.model.PhoneNumber
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneNumberDao {
    @Query("SELECT * FROM phone_numbers WHERE clientId = :clientId")
    fun getPhoneNumbersByClient(clientId: Long): Flow<List<PhoneNumber>>

    @Query("SELECT * FROM phone_numbers WHERE clientId = :clientId")
    suspend fun getPhoneNumbersByClientSync(clientId: Long): List<PhoneNumber>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneNumber(phoneNumber: PhoneNumber): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneNumbers(phoneNumbers: List<PhoneNumber>)

    @Update
    suspend fun updatePhoneNumber(phoneNumber: PhoneNumber)

    @Delete
    suspend fun deletePhoneNumber(phoneNumber: PhoneNumber)

    @Query("DELETE FROM phone_numbers WHERE clientId = :clientId")
    suspend fun deleteByClientId(clientId: Long)
}