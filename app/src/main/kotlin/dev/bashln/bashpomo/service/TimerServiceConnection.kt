package dev.bashln.bashpomo.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerServiceConnection : ServiceConnection {

    private val _service = MutableStateFlow<TimerService?>(null)
    val service: StateFlow<TimerService?> = _service.asStateFlow()

    val isBound: Boolean get() = _service.value != null

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        _service.value = (binder as TimerService.TimerBinder).getService()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        _service.value = null
    }
}
