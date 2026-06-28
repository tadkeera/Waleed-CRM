package com.pharmacomm.crm.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "specialties")
data class Specialty(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(tableName = "regions")
data class Region(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)