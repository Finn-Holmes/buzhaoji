package com.kexu.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.FocusSessionEntity
import com.kexu.app.data.model.HabitCheckEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.data.model.DayReflectionEntity
import com.kexu.app.data.model.TaskCompletionEntity

@Database(
    entities = [
        TaskEntity::class,
        CourseEntity::class,
        ScheduleSlotEntity::class,
        CourseOverrideEntity::class,
        HabitEntity::class,
        HabitCheckEntity::class,
        CountdownEntity::class,
        FocusSessionEntity::class,
        DayReflectionEntity::class,
        TaskCompletionEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class KexuDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun courseDao(): CourseDao
    abstract fun featureDao(): FeatureDao

    companion object {
        @Volatile private var instance: KexuDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN weekMask INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS course_overrides (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        slotId INTEGER NOT NULL,
                        weekNumber INTEGER NOT NULL,
                        isCancelled INTEGER NOT NULL,
                        dayOfWeek INTEGER,
                        startMinutes INTEGER,
                        endMinutes INTEGER,
                        location TEXT,
                        note TEXT NOT NULL,
                        FOREIGN KEY(slotId) REFERENCES schedule_slots(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_course_overrides_slotId ON course_overrides(slotId)")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_course_overrides_slotId_weekNumber
                    ON course_overrides(slotId, weekNumber)
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        symbol TEXT NOT NULL,
                        reminderMinutes INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habit_checks (
                        habitId INTEGER NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL,
                        PRIMARY KEY(habitId, dateEpochDay),
                        FOREIGN KEY(habitId) REFERENCES habits(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_checks_habitId ON habit_checks(habitId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_checks_dateEpochDay ON habit_checks(dateEpochDay)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS countdowns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        targetDateEpochDay INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        symbol TEXT NOT NULL,
                        repeatYearly INTEGER NOT NULL,
                        reminderDaysBefore INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_countdowns_targetDateEpochDay ON countdowns(targetDateEpochDay)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS focus_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER,
                        durationSeconds INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL,
                        mode TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_taskId ON focus_sessions(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_startedAt ON focus_sessions(startedAt)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN endAt INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS day_reflections (dateEpochDay INTEGER NOT NULL PRIMARY KEY, note TEXT NOT NULL, closedAt INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS task_completions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, taskId INTEGER NOT NULL, title TEXT NOT NULL, completedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_completions_completedAt ON task_completions(completedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_completions_taskId ON task_completions(taskId)")
                db.execSQL("INSERT INTO task_completions (taskId, title, completedAt) SELECT id, title, completedAt FROM tasks WHERE isCompleted = 1 AND completedAt IS NOT NULL")
            }
        }

        fun get(context: Context): KexuDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KexuDatabase::class.java,
                "kexu.db",
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
            ).build().also { instance = it }
        }
    }
}
