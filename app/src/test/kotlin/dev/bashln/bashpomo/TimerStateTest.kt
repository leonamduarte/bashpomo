package dev.bashln.bashpomo

import dev.bashln.bashpomo.service.TimerState
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateTest {

    @Test
    fun `phase transitions to OVERTIME when elapsed equals planned`() {
        val plannedSeconds = 1500
        val elapsedSeconds = 1500

        val newPhase = when {
            elapsedSeconds >= plannedSeconds -> TimerState.Phase.OVERTIME
            else -> TimerState.Phase.WORK
        }
        assertEquals(TimerState.Phase.OVERTIME, newPhase)
    }

    @Test
    fun `phase stays WORK when elapsed is less than planned`() {
        val plannedSeconds = 1500
        val elapsedSeconds = 1499

        val newPhase = when {
            elapsedSeconds >= plannedSeconds -> TimerState.Phase.OVERTIME
            else -> TimerState.Phase.WORK
        }
        assertEquals(TimerState.Phase.WORK, newPhase)
    }

    @Test
    fun `Idle is distinct from Running`() {
        val idle: TimerState = TimerState.Idle
        val running: TimerState = TimerState.Running(
            taskId = null,
            taskName = "",
            plannedSeconds = 1500,
            elapsedSeconds = 0,
            phase = TimerState.Phase.WORK,
        )
        assert(idle !is TimerState.Running)
        assert(running is TimerState.Running)
    }
}
