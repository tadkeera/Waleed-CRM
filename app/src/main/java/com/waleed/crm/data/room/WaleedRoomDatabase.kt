package com.waleed.crm.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClientEntity::class,
        MessageLogEntity::class,
        MessageCampaignEntity::class,
        FollowUpEntity::class,
        UserEntity::class,
        AuditLogEntity::class,
        SavedSegmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WaleedRoomDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun messageDao(): MessageDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun userDao(): UserDao
    abstract fun auditDao(): AuditDao
    abstract fun segmentDao(): SegmentDao

    companion object {
        @Volatile private var INSTANCE: WaleedRoomDatabase? = null

        fun getInstance(context: Context): WaleedRoomDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                WaleedRoomDatabase::class.java,
                "waleed_crm_room.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
