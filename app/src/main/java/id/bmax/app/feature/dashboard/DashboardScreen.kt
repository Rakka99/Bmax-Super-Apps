package id.bmax.app.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.bmax.app.core.ui.GlassCard

@Composable
fun DashboardScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bmax Super Apps")
                        Text(
                            "Electricity Payment Monitoring",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Operational Dashboard", style = MaterialTheme.typography.headlineSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    listOf(
                        "Pelanggan" to "0",
                        "Lunas" to "0",
                        "Belum Lunas" to "0",
                        "Collection Rate" to "0%"
                    )
                ) { (label, value) ->
                    GlassCard(Modifier.width(180.dp)) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(value, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text("RBM", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text("A  •  B  •  C  •  D  •  E")
                Text(
                    "Scope follows the authenticated Biller and Supabase RLS.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
