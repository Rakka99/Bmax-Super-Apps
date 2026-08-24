package id.bmax.app.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("Bmax Super Apps", style = MaterialTheme.typography.headlineMedium)
        Text("Login Biller", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(Modifier.fillMaxWidth().padding(top = 24.dp), email, viewModel::onEmailChanged, label = { Text("Email") }, singleLine = true)
        OutlinedTextField(Modifier.fillMaxWidth().padding(top = 12.dp), password, viewModel::onPasswordChanged, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        if (state is AuthState.Error) Text((state as AuthState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        Button(Modifier.fillMaxWidth().padding(top = 20.dp), enabled = state !is AuthState.Loading, onClick = viewModel::signIn) { Text("Masuk") }
        if (state is AuthState.Loading) CircularProgressIndicator(Modifier.padding(top = 16.dp))
    }
}
