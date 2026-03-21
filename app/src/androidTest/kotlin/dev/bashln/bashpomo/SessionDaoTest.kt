package dev.bashln.bashpomo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.bashln.bashpomo.data.db.AppDatabase
import dev.bashln.bashpomo.data.db.entity.SessionEntity
import dev.bashln.bashpomo.data.db.entity.TaskEntity
import dev.bashln.bashpomo.data.model.SessionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndQueryTask() = runTest {
        val taskId = db.taskDao().insert(TaskEntity(name = "Write tests"))
        val tasks = db.taskDao().getAllTasks()
        assertEquals(1, tasks.size)
        assertEquals("Write tests", tasks.first().name)
        assertEquals(taskId, tasks.first().id)
    }

    @Test
    fun insertAndQuerySession() = runTest {
        val taskId = db.taskDao().insert(TaskEntity(name = "Deep work"))
        val sessionId = db.sessionDao().insert(
            SessionEntity(
                taskId = taskId,
                type = SessionType.WORK,
                plannedSeconds = 1500,
                actualSeconds = 1680,
            )
        )
        val sessions = db.sessionDao().getAllSessions()
        assertEquals(1, sessions.size)
        val s = sessions.first()
        assertEquals(sessionId, s.id)
        assertEquals(taskId, s.taskId)
        assertEquals(1680, s.actualSeconds)
        assertEquals(SessionType.WORK, s.type)
    }

    @Test
    fun taskDeleteSetsSessionTaskIdNull() = runTest {
        val taskId = db.taskDao().insert(TaskEntity(name = "Deletable"))
        db.sessionDao().insert(
            SessionEntity(
                taskId = taskId,
                type = SessionType.WORK,
                plannedSeconds = 900,
                actualSeconds = 920,
            )
        )
        db.taskDao().delete(TaskEntity(id = taskId, name = "Deletable"))
        val sessions = db.sessionDao().getAllSessions()
        assertTrue(sessions.first().taskId == null)
    }
}
