package dev.bashln.bashpomo.data.repository

import dev.bashln.bashpomo.data.db.dao.SessionDao
import dev.bashln.bashpomo.data.db.dao.TaskDao
import dev.bashln.bashpomo.data.db.entity.SessionEntity
import dev.bashln.bashpomo.data.db.entity.TaskEntity
import dev.bashln.bashpomo.data.model.ExportRoot
import dev.bashln.bashpomo.data.model.SessionExportDto
import dev.bashln.bashpomo.data.model.TaskExportDto
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val sessionDao: SessionDao,
) {

    fun getActiveTasks(): Flow<List<TaskEntity>> = taskDao.getActiveTasks()

    suspend fun addTask(name: String): Long = taskDao.insert(TaskEntity(name = name))

    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

    suspend fun markTaskCompleted(taskId: Long) = taskDao.markCompleted(taskId)

    suspend fun saveSession(session: SessionEntity): Long = sessionDao.insert(session)

    suspend fun exportToJson(): String {
        val export = ExportRoot(
            exportedAt = System.currentTimeMillis(),
            tasks = taskDao.getAllTasks().map { it.toExportDto() },
            sessions = sessionDao.getAllSessions().map { it.toExportDto() },
        )
        return Json { prettyPrint = true }.encodeToString(ExportRoot.serializer(), export)
    }

    private fun TaskEntity.toExportDto() = TaskExportDto(
        id = id,
        name = name,
        isCompleted = isCompleted,
    )

    private fun SessionEntity.toExportDto() = SessionExportDto(
        id = id,
        taskId = taskId,
        type = type.name,
        plannedSeconds = plannedSeconds,
        actualSeconds = actualSeconds,
        timestamp = timestamp,
    )
}
