package com.waleed.crm.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClientEntity::class,
        SpecializationEntity::class,
        LocationEntity::class,
        PharmacyEntity::class,
        GalleryFileEntity::class,
        MessageTemplateEntity::class,
        MessageLogEntity::class,
        MessageCampaignEntity::class,
        FollowUpEntity::class,
        UserEntity::class,
        AuditLogEntity::class,
        SavedSegmentEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class WaleedRoomDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun catalogDao(): CatalogDao
    abstract fun galleryDao(): GalleryDao
    abstract fun messageDao(): MessageDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun userDao(): UserDao
    abstract fun auditDao(): AuditDao
    abstract fun segmentDao(): SegmentDao
    abstract fun dashboardDao(): DashboardDao

    companion object {
        private const val ROOM_DATABASE_NAME = "waleed_crm_room.db"
        @Volatile private var INSTANCE: WaleedRoomDatabase? = null

        fun getInstance(context: Context): WaleedRoomDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                WaleedRoomDatabase::class.java,
                ROOM_DATABASE_NAME
            )
                .addMigrations(*RoomMigrations.ALL)
                .build()
                .also { INSTANCE = it }
        }
    }
}
