package es.mixmat.listener.data.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthEvent @Inject constructor() {

    private val _tokenExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tokenExpired: SharedFlow<Unit> = _tokenExpired

    fun emitTokenExpired() {
        _tokenExpired.tryEmit(Unit)
    }
}
