package id.bmax.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val email: String?, val role: String = "BILLER") : AuthState
    data class Error(val message: String) : AuthState
    data class Info(val message: String) : AuthState
}

enum class AuthMode { LOGIN, SIGN_UP, FORGOT_PASSWORD }

@Serializable
private data class ProfileRoleDto(val role: String = "BILLER", val active: Boolean = true)

@HiltViewModel
class AuthViewModel @Inject constructor(private val supabase: SupabaseClient) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password
    private val _mode = MutableStateFlow(AuthMode.LOGIN)
    val mode: StateFlow<AuthMode> = _mode

    init { refreshSession() }

    fun onEmailChanged(value: String) { _email.value = value.trimStart() }
    fun onPasswordChanged(value: String) { _password.value = value }
    fun setMode(value: AuthMode) {
        _mode.value = value
        _state.value = AuthState.SignedOut
    }

    fun refreshSession() {
        viewModelScope.launch {
            val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
            _state.value = if (session == null) AuthState.SignedOut else loadSignedInState(session.user?.id, session.user?.email)
        }
    }

    fun submit() {
        when (_mode.value) {
            AuthMode.LOGIN -> signIn()
            AuthMode.SIGN_UP -> signUp()
            AuthMode.FORGOT_PASSWORD -> forgotPassword()
        }
    }

    private fun signIn() {
        val emailValue = _email.value.trim()
        val passwordValue = _password.value
        if (!validateCredentials(emailValue, passwordValue)) return

        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                supabase.auth.signInWith(Email) {
                    email = emailValue
                    password = passwordValue
                }
                val session = supabase.auth.currentSessionOrNull()
                _state.value = if (session == null) {
                    AuthState.Error("Login gagal: session Supabase tidak terbentuk.")
                } else {
                    loadSignedInState(session.user?.id, session.user?.email)
                }
            } catch (t: Throwable) {
                _state.value = AuthState.Error(toUserMessage(t))
            }
        }
    }

    private fun signUp() {
        val emailValue = _email.value.trim()
        val passwordValue = _password.value
        if (!validateCredentials(emailValue, passwordValue, requireStrongPassword = true)) return

        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                supabase.auth.signUpWith(Email) {
                    email = emailValue
                    password = passwordValue
                }
                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    _state.value = loadSignedInState(session.user?.id, session.user?.email)
                } else {
                    _state.value = AuthState.Info(
                        "Pendaftaran berhasil. Jika konfirmasi email aktif di Supabase, buka email konfirmasi terlebih dahulu lalu masuk."
                    )
                    _mode.value = AuthMode.LOGIN
                }
            } catch (t: Throwable) {
                _state.value = AuthState.Error(toUserMessage(t))
            }
        }
    }

    private fun forgotPassword() {
        val emailValue = _email.value.trim()
        if (emailValue.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            _state.value = AuthState.Error("Masukkan email yang valid untuk reset password.")
            return
        }

        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                supabase.auth.sendRecoveryEmail(email = emailValue)
                _state.value = AuthState.Info("Email reset password telah diminta. Periksa inbox/spam email Anda.")
                _mode.value = AuthMode.LOGIN
            } catch (t: Throwable) {
                _state.value = AuthState.Error(toUserMessage(t))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { supabase.auth.signOut() }
            _state.value = AuthState.SignedOut
            _mode.value = AuthMode.LOGIN
        }
    }

    private suspend fun loadSignedInState(userId: String?, email: String?): AuthState {
        if (userId.isNullOrBlank()) return AuthState.SignedIn(email, "BILLER")
        return runCatching {
            val profile = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeList<ProfileRoleDto>()
                .firstOrNull()
            if (profile != null && !profile.active) {
                AuthState.Error("Akun dinonaktifkan. Hubungi administrator Bmax.")
            } else {
                AuthState.SignedIn(email, profile?.role ?: "BILLER")
            }
        }.getOrElse {
            AuthState.SignedIn(email, "BILLER")
        }
    }

    private fun validateCredentials(emailValue: String, passwordValue: String, requireStrongPassword: Boolean = false): Boolean {
        if (emailValue.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            _state.value = AuthState.Error("Masukkan alamat email yang valid.")
            return false
        }
        if (passwordValue.isBlank()) {
            _state.value = AuthState.Error("Password wajib diisi.")
            return false
        }
        if (requireStrongPassword && passwordValue.length < 6) {
            _state.value = AuthState.Error("Password minimal 6 karakter.")
            return false
        }
        return true
    }

    private fun toUserMessage(t: Throwable): String {
        val raw = t.message.orEmpty()
        val normalized = raw.lowercase()
        return when {
            "invalid_credentials" in normalized || "invalid login credentials" in normalized ->
                "Email atau password salah. Pastikan akun sudah terdaftar di Supabase Authentication."
            "user already registered" in normalized || "already registered" in normalized ->
                "Email sudah terdaftar. Silakan masuk atau gunakan Lupa Password."
            "email not confirmed" in normalized || "email_not_confirmed" in normalized ->
                "Email belum dikonfirmasi. Buka email konfirmasi Supabase lalu coba login kembali."
            "rate limit" in normalized || "too many requests" in normalized ->
                "Terlalu banyak percobaan. Tunggu beberapa saat lalu coba lagi."
            "network" in normalized || "timeout" in normalized || "unable to resolve host" in normalized ->
                "Tidak dapat terhubung ke Supabase. Periksa koneksi internet lalu coba lagi."
            else -> "Operasi autentikasi gagal. Periksa data dan koneksi Supabase."
        }
    }
}
