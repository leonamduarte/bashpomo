package dev.bashln.bashpomo.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bashln.bashpomo.R
import dev.bashln.bashpomo.service.TimerState

@Composable
fun ControlRow(
    state: TimerState,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            is TimerState.Idle -> {
                Button(onClick = onStartWork, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_start_work))
                }
                OutlinedButton(onClick = onStartBreak, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_start_break))
                }
            }

            is TimerState.Running -> {
                OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_pause))
                }
                Button(onClick = onFinish, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_finish))
                }
            }

            is TimerState.Paused -> {
                Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_resume))
                }
                OutlinedButton(onClick = onFinish, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_finish))
                }
            }

            is TimerState.Finished -> {
                // Transitional — no controls shown
            }
        }
    }
}
