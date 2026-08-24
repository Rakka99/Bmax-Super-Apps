package id.bmax.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(28.dp)

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFFEAF4FF), Color(0xFFD8F4FF), Color(0xFFF4EEFF)))
        ).padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(shape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Bmax Super Apps", style = MaterialTheme.typography.headlineMedium)
            Text("Login Biller", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(Modifier.fillMaxWidth().padding(top = 24.dp), email, viewModel::onEmailChanged, label = { Text("Email") }, singleLine = true, shape = RoundedCornerShape(18.dp))
            OutlinedTextField(Modifier.fillMaxWidth().padding(top = 12.dp), password, viewModel::onPasswordChanged, label = { Text("Password") }, singleLine = true, shape = RoundedCornerShape(18.dp), visualTransformation = PasswordVisualTransformation())
            if (state is AuthState.Error) Text((state as AuthState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            Button(Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp), enabled = state !is AuthState.Loading, onClick = viewModel::signIn, shape = RoundedCornerShape(18.dp)) { Text("Masuk") }
            if (state is AuthState.Loading) CircularProgressIndicator(Modifier.padding(top = 16.dp))
        }
    }
}
