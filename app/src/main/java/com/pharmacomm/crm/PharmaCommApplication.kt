package com.pharmacomm.crm

import android.app.Application
import androidx.room.Room
import com.pharmacomm.crm.data.local.AppDatabase

class PharmaCommApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pharmacomm_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
}