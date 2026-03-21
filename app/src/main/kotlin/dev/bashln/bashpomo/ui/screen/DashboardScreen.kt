package dev.bashln.bashpomo.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bashln.bashpomo.R
import dev.bashln.bashpomo.service.TimerState
import dev.bashln.bashpomo.ui.component.ControlRow
import dev.bashln.bashpomo.ui.component.TaskSelector
import dev.bashln.bashpomo.ui.component.TimerDisplay
import dev.bashln.bashpomo.ui.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTaskId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Handle Finished state → show snackbar
    LaunchedEffect(timerState) {
        if (timerState is TimerState.Finished) {
            viewModel.onSessionFinishedSeen()
        }
    }

    // Consume UiEvents
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TimerViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is TimerViewModel.UiEvent.ShareJson -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, event.json)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export sessions"))
                }
            }
        }
    }

    val isIdle = timerState is TimerState.Idle || timerState is TimerState.Finished

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { viewModel.exportJson() }) {
                        Text(stringResource(R.string.label_export))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TimerDisplay(
                state = timerState,
                modifier = Modifier.fillMaxWidth(),
            )

            ControlRow(
                state = timerState,
                onStartWork = {
                    val task = activeTasks.find { it.id == selectedTaskId }
                    viewModel.startWork(
                        taskId = selectedTaskId,
                        taskName = task?.name ?: "",
                    )
                },
                onStartBreak = { viewModel.startBreak() },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onFinish = { viewModel.finish() },
            )

            if (isIdle) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                TaskSelector(
                    tasks = activeTasks,
                    selectedTaskId = selectedTaskId,
                    onTaskSelected = { selectedTaskId = it },
                    onAddTask = { viewModel.addTask(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                )
            }
        }
    }
}
