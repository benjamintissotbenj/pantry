package app.pantry.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _signedOut = MutableStateFlow(false)
    val signedOut = _signedOut.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            val result = auth.signOut()
            if (result.isSuccess) {
                _signedOut.value = true
            }
            // Phase 4: surface result.exceptionOrNull() via a snackbar / error state
        }
    }
}
