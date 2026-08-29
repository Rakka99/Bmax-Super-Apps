package id.bmax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import id.bmax.app.core.ui.BmaxTheme
import id.bmax.app.feature.auth.AuthScreen
import id.bmax.app.feature.auth.AuthState
import id.bmax.app.feature.auth.AuthViewModel
import id.bmax.app.feature.customer.CustomerDto
import id.bmax.app.feature.customer.CustomerScreen
import id.bmax.app.feature.customer.CustomerViewModel
import id.bmax.app.feature.dashboard.DashboardScreen
import id.bmax.app.feature.dashboard.DashboardViewModel
import id.bmax.app.feature.map.CustomerMapScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BmaxTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.state.collectAsStateWithLifecycle()
                var showCustomers by remember { mutableStateOf(false) }
                var selectedCustomer by remember { mutableStateOf<CustomerDto?>(null) }

                when (val state = authState) {
                    AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    AuthState.SignedOut, is AuthState.Error, is AuthState.Info -> AuthScreen(authViewModel)
                    is AuthState.SignedIn -> {
                        val customerViewModel: CustomerViewModel = hiltViewModel()
                        val dashboardViewModel: DashboardViewModel = hiltViewModel()

                        when {
                            selectedCustomer != null -> {
                                BackHandler { selectedCustomer = null }
                                val customer = selectedCustomer!!
                                CustomerMapScreen(customer.name, customer.latitude, customer.longitude) {
                                    selectedCustomer = null
                                }
                            }
                            showCustomers -> {
                                BackHandler { showCustomers = false }
                                CustomerScreen(customerViewModel, authViewModel::signOut) { selectedCustomer = it }
                            }
                            else -> {
                                DashboardScreen(
                                    dashboardViewModel = dashboardViewModel,
                                    customerViewModel = customerViewModel,
                                    email = state.email,
                                    role = state.role,
                                    onCustomers = { showCustomers = true },
                                    onLogout = authViewModel::signOut
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
