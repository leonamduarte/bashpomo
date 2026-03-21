package dev.bashln.bashpomo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExportRoot(
    val exportedAt: Long,
    val tasks: List<TaskExportDto>,
    val sessions: List<SessionExportDto>,
)

@Serializable
data class TaskExportDto(
    val id: Long,
    val name: String,
    val isCompleted: Boolean,
)

@Serializable
data class SessionExportDto(
    val id: Long,
    val taskId: Long?,
    val type: String,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val timestamp: Long,
)
