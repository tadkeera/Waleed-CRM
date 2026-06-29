package com.waleed.crm.data.room

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationInstrumentedTest {
    @Test
    fun migrationOneToTwoCreatesLegacyCoverageTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `clients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `second_phone` TEXT NOT NULL, `client_type` TEXT NOT NULL, `specialization` TEXT NOT NULL, `client_class` TEXT NOT NULL, `location` TEXT NOT NULL, `is_classified` INTEGER NOT NULL, `card_color` TEXT NOT NULL, `date_added` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `notes` TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `message_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `client_id` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `message_text` TEXT NOT NULL, `attachment_name` TEXT NOT NULL, `attachment_type` TEXT NOT NULL, `send_mode` TEXT NOT NULL, `campaign_id` INTEGER NOT NULL, `status` TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `message_campaigns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `target_count` INTEGER NOT NULL, `sent_count` INTEGER NOT NULL, `message_mode` TEXT NOT NULL, `attachment_name` TEXT NOT NULL, `date_created` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `follow_ups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `client_id` INTEGER NOT NULL, `title` TEXT NOT NULL, `due_at` INTEGER NOT NULL, `status` TEXT NOT NULL, `notes` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `username` TEXT NOT NULL, `password_hash` TEXT NOT NULL, `role` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `last_login` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `action` TEXT NOT NULL, `entity_type` TEXT NOT NULL, `entity_id` INTEGER NOT NULL, `entity_name` TEXT NOT NULL, `details` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `saved_segments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `query` TEXT NOT NULL, `client_type` TEXT NOT NULL, `specialization` TEXT NOT NULL, `location` TEXT NOT NULL, `client_class` TEXT NOT NULL, `only_pending_followup` INTEGER NOT NULL, `only_overdue_followup` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        RoomMigrations.MIGRATION_1_2.migrate(db)

        listOf("specializations", "locations", "pharmacies", "gallery_files", "message_templates").forEach { table ->
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
                assertTrue("$table should exist after migration", cursor.moveToFirst())
            }
        }
        helper.close()
    }
}
