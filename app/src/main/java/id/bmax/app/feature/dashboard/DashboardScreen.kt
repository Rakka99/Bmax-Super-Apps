package id.bmax.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.bmax.app.core.ui.GlassCard
import id.bmax.app.feature.customer.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    customerViewModel: CustomerViewModel,
    email: String?,
    role: String,
    onCustomers: () -> Unit,
    onLogout: () -> Unit,
) {
    val dashboard = dashboardViewModel.dashboard.collectAsStateWithLifecycle().value
    val loading = dashboardViewModel.loading.collectAsStateWithLifecycle().value
    val error = dashboardViewModel.error.collectAsStateWithLifecycle().value
    var period by remember { mutableStateOf<String?>(null) }
    val glass = RoundedCornerShape(24.dp)

    LaunchedEffect(Unit) { dashboardViewModel.refresh() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bmax Super Apps")
                        Text("Billing Dashboard", style = MaterialTheme.typography.labelMedium)
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

            GlassCard(Modifier.fillMaxWidth()) {
                Text("Periode Billing", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = period == null, onClick = { period = null; dashboardViewModel.refresh(null) }, label = { Text("Semua") })
                    FilterChip(selected = period == "202608", onClick = { period = "202608"; dashboardViewModel.refresh("202608") }, label = { Text("AGU 26") })
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    listOf(
                        "Pelanggan" to dashboard.total_customers.toString(),
                        "Tagihan" to dashboard.total_bills.toString(),
                        "Lunas" to dashboard.paid_bills.toString(),
                        "Belum Lunas" to dashboard.unpaid_bills.toString(),
                        "Collection" to "${dashboard.collection_rate}%"
                    )
                ) { (label, value) ->
                    GlassCard(Modifier.width(155.dp)) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(value, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text("Ringkasan Nominal", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Text("Total Tagihan  : Rp ${formatMoney(dashboard.total_billed)}")
                Text("Sudah Tertagih : Rp ${formatMoney(dashboard.total_paid)}")
                Text("Belum Tertagih : Rp ${formatMoney(dashboard.total_unpaid)}")
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text("Akses Cepat", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onCustomers, modifier = Modifier.fillMaxWidth(), shape = glass) {
                    Text("Data Pelanggan & Peta")
                }
                OutlinedButton(onClick = { dashboardViewModel.refresh(period) }, modifier = Modifier.fillMaxWidth(), shape = glass) {
                    Text(if (loading) "Memuat data..." else "Refresh Dashboard")
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text("Status Sistem", style = MaterialTheme.typography.titleMedium)
                Text("Supabase Auth • Billing RPC • Customer Data • Maps", style = MaterialTheme.typography.bodyMedium)
                Text("Scope dashboard mengikuti akun dan RLS Supabase.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatMoney(value: Double): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(value)
