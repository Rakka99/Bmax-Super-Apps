package id.bmax.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val glassShape = RoundedCornerShape(30.dp)
    val busy: Boolean = state is AuthState.Loading

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFFEAF4FF), Color(0xFFDCEBFF), Color(0xFFF5EEFF))))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(glassShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
                .border(1.dp, Color.White.copy(alpha = 0.75f), glassShape)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bmax Super Apps", style = MaterialTheme.typography.headlineMedium)
            Text(
                when (mode) {
                    AuthMode.LOGIN -> "Login Biller"
                    AuthMode.SIGN_UP -> "Daftar Akun"
                    AuthMode.FORGOT_PASSWORD -> "Lupa Password"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(22.dp))

            OutlinedTextField(
                value = email,
                onValueChange = viewModel::onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                placeholder = { Text("nama@email.com") },
                singleLine = true,
                enabled = busy == false,
                shape = RoundedCornerShape(20.dp)
            )

            if (mode != AuthMode.FORGOT_PASSWORD) {
                OutlinedTextField(
                    value = password,
                    onValueChange = viewModel::onPasswordChanged,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    label = { Text("Password") },
                    placeholder = { Text("Minimal 6 karakter") },
                    singleLine = true,
                    enabled = busy == false,
                    shape = RoundedCornerShape(20.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = passwordVisible == false }, enabled = busy == false) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                "Tampilkan password"
                            )
                        }
                    }
                )
            }

            when (state) {
                is AuthState.Error -> Text(
                    (state as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                is AuthState.Info -> Text(
                    (state as AuthState.Info).message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                else -> Unit
            }

            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(54.dp),
                enabled = busy == false,
                onClick = viewModel::submit,
                shape = RoundedCornerShape(20.dp)
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(
                    when (mode) {
                        AuthMode.LOGIN -> "Masuk"
                        AuthMode.SIGN_UP -> "Daftar"
                        AuthMode.FORGOT_PASSWORD -> "Kirim Link Reset"
                    }
                )
            }

            if (mode == AuthMode.LOGIN) {
                TextButton(enabled = busy == false, onClick = { viewModel.setMode(AuthMode.FORGOT_PASSWORD) }) {
                    Text("Lupa password?")
                }
                OutlinedButton(
                    enabled = busy == false,
                    onClick = { viewModel.setMode(AuthMode.SIGN_UP) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Belum punya akun? Daftar")
                }
            } else {
                TextButton(enabled = busy == false, onClick = { viewModel.setMode(AuthMode.LOGIN) }) {
                    Text("Kembali ke Login")
                }
            }

            Text("Akses menggunakan Supabase Authentication", style = MaterialTheme.typography.labelSmall)
        }
    }
}
