package br.com.leogsouza.escalav.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionEvents @Inject constructor() {
    private val _forcedLogout = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val forcedLogout: SharedFlow<Unit> = _forcedLogout

    fun emitForcedLogout() {
        _forcedLogout.tryEmit(Unit)
    }
}

