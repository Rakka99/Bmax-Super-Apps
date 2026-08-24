package id.bmax.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val email: String?) : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient,
) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    init { refreshSession() }
    fun onEmailChanged(value: String) { _email.value = value }
    fun onPasswordChanged(value: String) { _password.value = value }

    fun refreshSession() {
        viewModelScope.launch {
            val session = runCatching { supabase.gotrue.currentSessionOrNull() }.getOrNull()
            _state.value = if (session == null) AuthState.SignedOut else AuthState.SignedIn(session.user?.email)
        }
    }

    fun signIn() {
        val emailValue = _email.value.trim()
        if (emailValue.isBlank() || _password.value.isBlank()) {
            _state.value = AuthState.Error("Email dan password wajib diisi.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            runCatching {
                supabase.gotrue.loginWith(Email) {
                    email = emailValue
                    password = _password.value
                }
            }.onSuccess {
                _state.value = AuthState.SignedIn(supabase.gotrue.currentSessionOrNull()?.user?.email)
            }.onFailure { error ->
                _state.value = AuthState.Error(error.message ?: "Login gagal.")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { supabase.gotrue.logout() }
            _state.value = AuthState.SignedOut
        }
    }
}
