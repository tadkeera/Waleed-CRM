package com.waleed.crm.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    exportSchema = false
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

    companion object {
        private const val ROOM_DATABASE_NAME = "waleed_crm_room.db"
        @Volatile private var INSTANCE: WaleedRoomDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `specializations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_specializations_name` ON `specializations` (`name`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `locations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_locations_name` ON `locations` (`name`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `pharmacies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `client_id` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pharmacies_client_id` ON `pharmacies` (`client_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pharmacies_name` ON `pharmacies` (`name`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `gallery_files` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `file_path` TEXT NOT NULL, `type` TEXT NOT NULL, `date_added` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_files_date_added` ON `gallery_files` (`date_added`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_files_type` ON `gallery_files` (`type`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `message_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `date_added` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_templates_date_added` ON `message_templates` (`date_added`)")
            }
        }

        fun getInstance(context: Context): WaleedRoomDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                WaleedRoomDatabase::class.java,
                ROOM_DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
        }
    }
}
