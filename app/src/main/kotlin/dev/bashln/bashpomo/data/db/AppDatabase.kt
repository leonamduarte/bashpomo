package dev.bashln.bashpomo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dev.bashln.bashpomo.data.db.dao.SessionDao
import dev.bashln.bashpomo.data.db.dao.TaskDao
import dev.bashln.bashpomo.data.db.entity.SessionEntity
import dev.bashln.bashpomo.data.db.entity.TaskEntity
import dev.bashln.bashpomo.data.model.SessionType

@Database(
    entities = [TaskEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(SessionTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
}

class SessionTypeConverter {
    @TypeConverter
    fun fromSessionType(type: SessionType): String = type.name

    @TypeConverter
    fun toSessionType(value: String): SessionType = SessionType.valueOf(value)
}
