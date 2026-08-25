package id.bmax.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.bmax.app.core.ui.GlassCard
import id.bmax.app.feature.customer.CustomerViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CustomerViewModel,
    email: String?,
    role: String,
    onCustomers: () -> Unit,
    onLogout: () -> Unit,
) {
    val customers = viewModel.customers.collectAsStateWithLifecycle().value
    val loading = viewModel.loading.collectAsStateWithLifecycle().value
    val error = viewModel.error.collectAsStateWithLifecycle().value
    val paid = customers.count { it.status.equals("PAID", true) || it.status.equals("LUNAS", true) }
    val unpaid = (customers.size - paid).coerceAtLeast(0)
    val collection = if (customers.isEmpty()) 0 else ((paid * 100) / customers.size)
    val glass = RoundedCornerShape(24.dp)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bmax Super Apps")
                        Text("Dashboard", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = { TextButton(onClick = onLogout) { Text("Keluar") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFEAF4FF), Color(0xFFF5EEFF))))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Selamat datang", style = MaterialTheme.typography.labelLarge)
                Text(email ?: "Pengguna Bmax", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                AssistChip(onClick = {}, label = { Text(role.uppercase()) })
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    listOf(
                        "Pelanggan" to customers.size.toString(),
                        "Lunas" to paid.toString(),
                        "Belum Lunas" to unpaid.toString(),
                        "Collection" to "$collection%"
                    )
                ) { (label, value) ->
                    GlassCard(Modifier.width(165.dp)) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(value, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text("Akses Cepat", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onCustomers, modifier = Modifier.fillMaxWidth(), shape = glass) {
                    Text("Data Pelanggan & Peta")
                }
                OutlinedButton(onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth(), shape = glass) {
                    Text(if (loading) "Memuat data..." else "Refresh Data")
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text("Status Sistem", style = MaterialTheme.typography.titleMedium)
                Text("Supabase Authentication • Customer Data • Maps", style = MaterialTheme.typography.bodyMedium)
                Text("Data mengikuti akun dan kebijakan RLS Supabase.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
