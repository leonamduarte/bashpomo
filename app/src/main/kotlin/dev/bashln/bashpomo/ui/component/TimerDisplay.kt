package dev.bashln.bashpomo.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bashln.bashpomo.service.TimerState
import dev.bashln.bashpomo.service.remainingSeconds
import dev.bashln.bashpomo.ui.theme.BreakPurple
import dev.bashln.bashpomo.ui.theme.OvertimeOrange
import dev.bashln.bashpomo.ui.theme.WorkGreen
import dev.bashln.bashpomo.util.TimeFormatUtil

@Composable
fun TimerDisplay(state: TimerState, modifier: Modifier = Modifier) {
    val targetColor = phaseColor(state)
    val bgColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 600),
        label = "timer_bg",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatDisplay(state),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

private fun phaseColor(state: TimerState): Color = when (state) {
    is TimerState.Running -> when (state.phase) {
        TimerState.Phase.WORK -> WorkGreen
        TimerState.Phase.OVERTIME -> OvertimeOrange
        TimerState.Phase.BREAK -> BreakPurple
    }
    is TimerState.Paused -> when (state.phase) {
        TimerState.Phase.WORK -> WorkGreen.copy(alpha = 0.6f)
        TimerState.Phase.OVERTIME -> OvertimeOrange.copy(alpha = 0.6f)
        TimerState.Phase.BREAK -> BreakPurple.copy(alpha = 0.6f)
    }
    else -> Color.DarkGray
}

private fun formatDisplay(state: TimerState): String = when (state) {
    is TimerState.Running -> when (state.phase) {
        TimerState.Phase.WORK -> TimeFormatUtil.formatMMSS(state.remainingSeconds().coerceAtLeast(0))
        TimerState.Phase.OVERTIME -> "+${TimeFormatUtil.formatMMSS(state.elapsedSeconds - state.plannedSeconds)}"
        TimerState.Phase.BREAK -> TimeFormatUtil.formatMMSS(state.remainingSeconds().coerceAtLeast(0))
    }
    is TimerState.Paused -> when (state.phase) {
        TimerState.Phase.WORK -> TimeFormatUtil.formatMMSS(state.remainingSeconds().coerceAtLeast(0))
        TimerState.Phase.OVERTIME -> "+${TimeFormatUtil.formatMMSS(state.elapsedSeconds - state.plannedSeconds)}"
        TimerState.Phase.BREAK -> TimeFormatUtil.formatMMSS(state.remainingSeconds().coerceAtLeast(0))
    }
    is TimerState.Finished -> TimeFormatUtil.formatMMSS(0)
    TimerState.Idle -> "25:00"
}
