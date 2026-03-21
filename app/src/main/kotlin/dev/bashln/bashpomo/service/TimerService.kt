package dev.bashln.bashpomo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bashln.bashpomo.R
import dev.bashln.bashpomo.data.db.entity.SessionEntity
import dev.bashln.bashpomo.data.model.SessionType
import dev.bashln.bashpomo.data.repository.DataRepository
import dev.bashln.bashpomo.ui.MainActivity
import dev.bashln.bashpomo.util.TimeFormatUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    companion object {
        const val CHANNEL_ONGOING = "ch_timer_ongoing"
        const val CHANNEL_ALERT = "ch_timer_alert"
        const val NOTIFICATION_ID_ONGOING = 1
        const val NOTIFICATION_ID_ALERT = 2

        const val ACTION_PAUSE = "dev.bashln.bashpomo.PAUSE"
        const val ACTION_RESUME = "dev.bashln.bashpomo.RESUME"
        const val ACTION_FINISH = "dev.bashln.bashpomo.FINISH"

        private val _timerStateFlow: MutableStateFlow<TimerState> =
            MutableStateFlow(TimerState.Idle)

        /** Single source of truth — survives config changes. */
        val timerStateFlow: StateFlow<TimerState> = _timerStateFlow.asStateFlow()
    }

    @Inject
    lateinit var repository: DataRepository

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_FINISH -> serviceScope.launch { finishInternal() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.launch {
            val state = _timerStateFlow.value
            if (state is TimerState.Running || state is TimerState.Paused) {
                finishInternal()
            }
        }
    }

    // ── Public control API ────────────────────────────────────────────────────

    fun startWork(taskId: Long?, taskName: String, plannedSeconds: Int) {
        startTimer(
            taskId = taskId,
            taskName = taskName,
            plannedSeconds = plannedSeconds,
            phase = TimerState.Phase.WORK,
        )
    }

    fun startBreak(plannedSeconds: Int) {
        startTimer(
            taskId = null,
            taskName = "",
            plannedSeconds = plannedSeconds,
            phase = TimerState.Phase.BREAK,
        )
    }

    fun pause() {
        val state = _timerStateFlow.value
        if (state is TimerState.Running) {
            tickerJob?.cancel()
            _timerStateFlow.value = TimerState.Paused(
                taskId = state.taskId,
                taskName = state.taskName,
                plannedSeconds = state.plannedSeconds,
                elapsedSeconds = state.elapsedSeconds,
                phase = state.phase,
            )
            updateOngoingNotification()
        }
    }

    fun resume() {
        val state = _timerStateFlow.value
        if (state is TimerState.Paused) {
            _timerStateFlow.value = TimerState.Running(
                taskId = state.taskId,
                taskName = state.taskName,
                plannedSeconds = state.plannedSeconds,
                elapsedSeconds = state.elapsedSeconds,
                phase = state.phase,
            )
            launchTicker()
        }
    }

    suspend fun finishInternal() {
        tickerJob?.cancel()
        val state = _timerStateFlow.value
        val elapsed: Int
        val planned: Int
        val taskId: Long?
        val phase: TimerState.Phase

        when (state) {
            is TimerState.Running -> {
                elapsed = state.elapsedSeconds
                planned = state.plannedSeconds
                taskId = state.taskId
                phase = state.phase
            }
            is TimerState.Paused -> {
                elapsed = state.elapsedSeconds
                planned = state.plannedSeconds
                taskId = state.taskId
                phase = state.phase
            }
            else -> {
                _timerStateFlow.value = TimerState.Idle
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
        }

        val sessionType = if (phase == TimerState.Phase.BREAK) SessionType.BREAK else SessionType.WORK
        // Overtime is included in actualSeconds — not deducted from next session
        val actualSeconds = elapsed

        val sessionId = repository.saveSession(
            SessionEntity(
                taskId = taskId,
                type = sessionType,
                plannedSeconds = planned,
                actualSeconds = actualSeconds,
            )
        )

        _timerStateFlow.value = TimerState.Finished(
            sessionId = sessionId,
            actualSeconds = actualSeconds,
        )

        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Brief pause so observers see Finished before Idle
        delay(100)
        _timerStateFlow.value = TimerState.Idle
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun startTimer(
        taskId: Long?,
        taskName: String,
        plannedSeconds: Int,
        phase: TimerState.Phase,
    ) {
        _timerStateFlow.value = TimerState.Running(
            taskId = taskId,
            taskName = taskName,
            plannedSeconds = plannedSeconds,
            elapsedSeconds = 0,
            phase = phase,
        )
        acquireWakeLock()
        startForeground(NOTIFICATION_ID_ONGOING, buildOngoingNotification())
        launchTicker()
    }

    private fun launchTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (true) {
                delay(1_000)
                val current = _timerStateFlow.value
                if (current !is TimerState.Running) break

                val newElapsed = current.elapsedSeconds + 1
                val newPhase = when {
                    current.phase == TimerState.Phase.WORK && newElapsed >= current.plannedSeconds ->
                        TimerState.Phase.OVERTIME
                    else -> current.phase
                }

                if (newPhase == TimerState.Phase.OVERTIME && current.phase == TimerState.Phase.WORK) {
                    fireAlertNotification()
                }

                _timerStateFlow.value = current.copy(
                    elapsedSeconds = newElapsed,
                    phase = newPhase,
                )
                updateOngoingNotification()
            }
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                getString(R.string.notification_channel_timer_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_timer_desc)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                getString(R.string.notification_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_channel_alert_desc)
            }
        )
    }

    private fun buildOngoingNotification(): Notification {
        val state = _timerStateFlow.value
        val contentText = when (state) {
            is TimerState.Running -> buildTimeText(state.elapsedSeconds, state.plannedSeconds, state.phase)
            is TimerState.Paused -> buildTimeText(state.elapsedSeconds, state.plannedSeconds, state.phase) + " (paused)"
            else -> "—"
        }

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (state is TimerState.Running) {
            builder.addAction(
                0,
                getString(R.string.notification_action_pause),
                actionIntent(ACTION_PAUSE),
            )
        } else if (state is TimerState.Paused) {
            builder.addAction(
                0,
                getString(R.string.notification_action_resume),
                actionIntent(ACTION_RESUME),
            )
        }

        builder.addAction(
            0,
            getString(R.string.notification_action_finish),
            actionIntent(ACTION_FINISH),
        )

        return builder.build()
    }

    private fun updateOngoingNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_ONGOING, buildOngoingNotification())
    }

    private fun fireAlertNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(getString(R.string.notification_time_up_title))
            .setContentText(getString(R.string.notification_time_up_text))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID_ALERT, notification)
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildTimeText(elapsed: Int, planned: Int, phase: TimerState.Phase): String {
        return when (phase) {
            TimerState.Phase.WORK -> TimeFormatUtil.formatMMSS(planned - elapsed)
            TimerState.Phase.OVERTIME -> "+${TimeFormatUtil.formatMMSS(elapsed - planned)}"
            TimerState.Phase.BREAK -> TimeFormatUtil.formatMMSS(planned - elapsed)
        }
    }

    // ── WakeLock ──────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "bashpomo:TimerWakeLock",
        ).also { it.acquire(4 * 60 * 60 * 1000L /* 4 hours max */) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }
}
