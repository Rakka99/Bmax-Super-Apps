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
    val identifier by viewModel.identifier.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val loginRole by viewModel.loginRole.collectAsStateWithLifecycle()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val busy = state is AuthState.Loading
    val glassShape = RoundedCornerShape(30.dp)
    val isBiller = loginRole == LoginRole.BILLER

    Box(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF081326), Color(0xFF111A31), Color(0xFF081326))))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(glassShape)
                .background(Color(0xFF20283A).copy(alpha = 0.94f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), glassShape)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BMAX PLN", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("Biller Management & Execution SuperApp", color = Color.LightGray)
            Spacer(Modifier.height(20.dp))

            if (mode == AuthMode.LOGIN) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleButton("ADMIN", LoginRole.ADMIN, loginRole, busy) { viewModel.setLoginRole(it) }
                    RoleButton("SUPERVISOR", LoginRole.SUPERVISOR, loginRole, busy) { viewModel.setLoginRole(it) }
                    RoleButton("BILLER", LoginRole.BILLER, loginRole, busy) { viewModel.setLoginRole(it) }
                }
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = identifier,
                onValueChange = viewModel::onIdentifierChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isBiller) "Username Biller" else "Email") },
                placeholder = { Text(if (isBiller) "contoh: 53511.wildan" else "admin@contoh.id") },
                singleLine = true,
                enabled = !busy,
                shape = RoundedCornerShape(18.dp),
            )

            if (mode != AuthMode.FORGOT_PASSWORD) {
                OutlinedTextField(
                    value = password,
                    onValueChange = viewModel::onPasswordChanged,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    label = { Text("Kata Sandi") },
                    placeholder = { Text(if (isBiller) "Password default: biller" else "Password akun") },
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(18.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !busy) {
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
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                is AuthState.Info -> Text(
                    (state as AuthState.Info).message,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                else -> Unit
            }

            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(54.dp),
                enabled = !busy,
                onClick = viewModel::submit,
                shape = RoundedCornerShape(18.dp)
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(if (mode == AuthMode.LOGIN) "MASUK" else if (mode == AuthMode.SIGN_UP) "DAFTAR" else "KIRIM LINK RESET")
            }

            if (mode == AuthMode.LOGIN) {
                TextButton(enabled = !busy, onClick = { viewModel.setMode(AuthMode.FORGOT_PASSWORD) }) { Text("Lupa Password?") }
                if (!isBiller) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { viewModel.setMode(AuthMode.SIGN_UP) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("Daftar akun Admin/Supervisor") }
                }
            } else {
                TextButton(enabled = !busy, onClick = { viewModel.setMode(AuthMode.LOGIN) }) { Text("Kembali ke Login") }
            }

            Text("Supabase Auth • Akun wajib terdaftar dan aktif", color = Color.LightGray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun RoleButton(label: String, role: LoginRole, selected: LoginRole, enabled: Boolean, onClick: (LoginRole) -> Unit) {
    Button(
        onClick = { onClick(role) },
        enabled = enabled,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected == role) MaterialTheme.colorScheme.primary else Color(0xFF303746),
            contentColor = Color.White
        )
    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
}
