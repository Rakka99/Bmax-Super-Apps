package id.bmax.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val email: String?, val role: String = "biller") : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(private val supabase: SupabaseClient) : ViewModel() {
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
            val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
            _state.value = if (session == null) {
                AuthState.SignedOut
            } else {
                AuthState.SignedIn(
                    email = session.user?.email,
                    role = readRole(session.user)
                )
            }
        }
    }

    fun signIn() {
        val emailValue = _email.value.trim()
        val passwordValue = _password.value

        if (emailValue.isBlank()) {
            _state.value = AuthState.Error("Email wajib diisi.")
            return
        }
        if (passwordValue.isBlank()) {
            _state.value = AuthState.Error("Password wajib diisi.")
            return
        }

        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                supabase.auth.signInWith(Email) {
                    email = emailValue
                    password = passwordValue
                }

                val session = supabase.auth.currentSessionOrNull()
                if (session == null) {
                    _state.value = AuthState.Error("Login gagal: session Supabase tidak terbentuk.")
                } else {
                    _state.value = AuthState.SignedIn(
                        email = session.user?.email,
                        role = readRole(session.user)
                    )
                }
            } catch (t: Throwable) {
                _state.value = AuthState.Error(toUserMessage(t))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { supabase.auth.signOut() }
            _state.value = AuthState.SignedOut
        }
    }

    private fun readRole(user: Any?): String {
        // Authentication is intentionally role-agnostic: admin, biller and viewer
        // accounts are all allowed to authenticate. Role authorization can be added
        // server-side without preventing a valid Supabase Auth login.
        return "biller"
    }

    private fun toUserMessage(t: Throwable): String {
        val raw = t.message.orEmpty()
        val normalized = raw.lowercase()

        return when {
            "invalid_credentials" in normalized || "invalid login credentials" in normalized ->
                "Email atau password salah, atau akun belum terdaftar di Supabase Authentication. Pastikan akun dibuat di Authentication > Users dan passwordnya benar."
            "email not confirmed" in normalized || "email_not_confirmed" in normalized ->
                "Email belum dikonfirmasi. Buka email konfirmasi Supabase lalu coba login kembali."
            "rate limit" in normalized || "too many requests" in normalized ->
                "Terlalu banyak percobaan login. Tunggu beberapa saat lalu coba lagi."
            "network" in normalized || "timeout" in normalized || "unable to resolve host" in normalized ->
                "Tidak dapat terhubung ke server Supabase. Periksa koneksi internet lalu coba lagi."
            else -> "Login gagal. Silakan periksa email/password dan koneksi Supabase."
        }
    }
}
