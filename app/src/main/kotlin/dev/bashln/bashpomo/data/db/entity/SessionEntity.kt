package dev.bashln.bashpomo.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.bashln.bashpomo.data.model.SessionType

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL,
        )
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "task_id", index = true)
    val taskId: Long?,
    val type: SessionType,
    @ColumnInfo(name = "planned_seconds")
    val plannedSeconds: Int,
    @ColumnInfo(name = "actual_seconds")
    val actualSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
)
