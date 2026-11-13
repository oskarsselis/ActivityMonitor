package com.example.activitycounter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.activitycounter.model.ActivityItem
import com.example.activitycounter.model.Note
import com.example.activitycounter.model.TaskGroup
import com.example.activitycounter.model.TaskItem
import com.example.activitycounter.model.startOfCurrentWeekMillis

@Database(
    entities = [ActivityItem::class, Note::class, TaskItem::class, TaskGroup::class],
    version = 6,
    exportSchema = false
)
abstract class ActivityDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: ActivityDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE activities ADD COLUMN lastIncrementTimestamp INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // Drops the count column and switches to per-activity periods. SQLite on older
        // Android can't drop columns, so the table is rebuilt. Existing activities become
        // weekly and start fresh, except ones already incremented this week stay completed.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    CREATE TABLE activities_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        periodDays INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastCompletedAt INTEGER NOT NULL,
                        previousCompletedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO activities_new (id, name, periodDays, createdAt, lastCompletedAt, previousCompletedAt)
                    SELECT id, name, 7, ?,
                        CASE WHEN lastIncrementTimestamp >= ? THEN lastIncrementTimestamp ELSE 0 END,
                        0
                    FROM activities
                    """.trimIndent(),
                    arrayOf<Any>(now, startOfCurrentWeekMillis(now))
                )
                db.execSQL("DROP TABLE activities")
                db.execSQL("ALTER TABLE activities_new RENAME TO activities")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        isChecklist INTEGER NOT NULL,
                        colorIndex INTEGER NOT NULL,
                        isPinned INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        isDone INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        isCollapsed INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // Existing tasks start without a group
                db.execSQL("ALTER TABLE tasks ADD COLUMN groupId INTEGER")
            }
        }

        fun getDatabase(context: Context): ActivityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ActivityDatabase::class.java,
                    "activity_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}