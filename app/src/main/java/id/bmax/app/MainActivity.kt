package id.bmax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import id.bmax.app.core.ui.BmaxTheme
import id.bmax.app.feature.auth.AuthScreen
import id.bmax.app.feature.auth.AuthState
import id.bmax.app.feature.auth.AuthViewModel
import id.bmax.app.feature.customer.CustomerScreen
import id.bmax.app.feature.customer.CustomerViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BmaxTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState = authViewModel.state.value
                when (authState) {
                    AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    AuthState.SignedOut, is AuthState.Error -> AuthScreen(authViewModel)
                    is AuthState.SignedIn -> {
                        val customerViewModel: CustomerViewModel = hiltViewModel()
                        CustomerScreen(customerViewModel, authViewModel::signOut)
                    }
                }
            }
        }
    }
}
