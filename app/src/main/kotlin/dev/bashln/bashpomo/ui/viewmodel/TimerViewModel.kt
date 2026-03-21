package dev.bashln.bashpomo.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bashln.bashpomo.data.db.entity.TaskEntity
import dev.bashln.bashpomo.data.repository.DataRepository
import dev.bashln.bashpomo.service.TimerService
import dev.bashln.bashpomo.service.TimerServiceConnection
import dev.bashln.bashpomo.service.TimerState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    application: Application,
    private val repository: DataRepository,
) : AndroidViewModel(application) {

    private val connection = TimerServiceConnection()

    val timerState: StateFlow<TimerState> = TimerService.timerStateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, TimerState.Idle)

    val activeTasks: StateFlow<List<TaskEntity>> = repository.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        bindService()
    }

    override fun onCleared() {
        super.onCleared()
        if (connection.isBound) {
            getApplication<Application>().unbindService(connection)
        }
    }

    // ── Control ───────────────────────────────────────────────────────────────

    fun startWork(taskId: Long?, taskName: String, plannedSeconds: Int = 25 * 60) {
        ensureServiceStarted()
        connection.service.value?.startWork(taskId, taskName, plannedSeconds)
    }

    fun startBreak(plannedSeconds: Int = 5 * 60) {
        ensureServiceStarted()
        connection.service.value?.startBreak(plannedSeconds)
    }

    fun pause() {
        connection.service.value?.pause()
    }

    fun resume() {
        connection.service.value?.resume()
    }

    fun finish() {
        viewModelScope.launch {
            connection.service.value?.finishInternal()
        }
    }

    fun addTask(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addTask(name.trim()) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun exportJson() {
        viewModelScope.launch {
            val json = repository.exportToJson()
            _events.emit(UiEvent.ShareJson(json))
        }
    }

    fun onSessionFinishedSeen() {
        viewModelScope.launch { _events.emit(UiEvent.ShowSnackbar("Session saved")) }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun ensureServiceStarted() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TimerService::class.java)
        ctx.startForegroundService(intent)
    }

    private fun bindService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TimerService::class.java)
        ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    sealed class UiEvent {
        data class ShareJson(val json: String) : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
