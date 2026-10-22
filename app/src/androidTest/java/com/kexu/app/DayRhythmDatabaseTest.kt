package com.kexu.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kexu.app.data.KexuDatabase
import com.kexu.app.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayRhythmDatabaseTest {
    @Test fun completionUndoRecurrenceAndDuplicateFocus() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,KexuDatabase::class.java).build()
        try {
            val dao=db.taskDao()
            val draft=TaskEntity(title="one")
            val original=draft.copy(id=dao.insert(draft))
            val done=original.copy(isCompleted=true,completedAt=1000L)
            assertTrue(dao.updateIfUnchanged(original,done))
            assertFalse(dao.updateIfUnchanged(original,done))
            assertEquals(1,dao.getCompletions().size)
            assertTrue(dao.updateIfUnchanged(done,original))
            assertEquals(0,dao.getCompletions().size)
            val recurring=original.copy(dueAt=10000L,recurrenceRule="DAILY")
            dao.update(recurring)
            val next=recurring.copy(dueAt=20000L)
            assertTrue(dao.updateIfUnchanged(recurring,next))
            assertEquals(1,dao.getCompletions().size)
            dao.delete(next)
            assertEquals(1,dao.getCompletions().size)
            val session=FocusSessionEntity(durationSeconds=60,startedAt=123L,completedAt=456L)
            db.featureDao().saveFocusSessionOnce(session)
            db.featureDao().saveFocusSessionOnce(session)
            assertEquals(1,db.featureDao().getFocusSessions().size)
            db.featureDao().saveReflection(DayReflectionEntity(20000L,"今天很不错",999L))
        } finally {db.close()}
    }
}
