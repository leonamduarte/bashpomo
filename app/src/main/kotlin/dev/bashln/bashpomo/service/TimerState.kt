package dev.bashln.bashpomo.service

sealed class TimerState {

    data object Idle : TimerState()

    data class Running(
        val taskId: Long?,
        val taskName: String,
        val plannedSeconds: Int,
        val elapsedSeconds: Int,
        val phase: Phase,
    ) : TimerState()

    data class Paused(
        val taskId: Long?,
        val taskName: String,
        val plannedSeconds: Int,
        val elapsedSeconds: Int,
        val phase: Phase,
    ) : TimerState()

    /**
     * Emitted briefly after finishInternal() completes before transitioning back to Idle.
     * Carries the persisted session id so the UI can show a "Session saved" snackbar.
     */
    data class Finished(
        val sessionId: Long,
        val actualSeconds: Int,
    ) : TimerState()

    enum class Phase {
        WORK,
        OVERTIME,
        BREAK,
    }
}

/** Remaining display seconds (negative means overtime). */
fun TimerState.Running.remainingSeconds(): Int = plannedSeconds - elapsedSeconds

fun TimerState.Paused.remainingSeconds(): Int = plannedSeconds - elapsedSeconds
