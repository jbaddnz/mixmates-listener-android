package es.mixmat.listener.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.repository.AuthRepository
import javax.inject.Inject

/**
 * App-root view model: only the start-destination token check. Deliberately
 * NOT TokenEntryViewModel — a second instance of that at the app root would
 * also collect the Apple sign-in returns and race the sign-in screen's
 * instance for them (it won, invisibly, once).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    fun hasToken(): Boolean = authRepository.hasToken()
}
