package id.bmax.app.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(viewModel: CustomerViewModel, onLogout: () -> Unit, onShowMap: (CustomerDto) -> Unit) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    val filtered = if (search.isBlank()) customers else customers.filter { it.idpel.contains(search, true) || it.name.contains(search, true) }
    val glassShape = RoundedCornerShape(22.dp)
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Column { Text("Bmax Super Apps"); Text("Pelanggan", style = MaterialTheme.typography.labelMedium) } }, actions = { TextButton(onClick = onLogout) { Text("Keluar") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface))).padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Cari IDPEL / nama") }, singleLine = true, shape = RoundedCornerShape(18.dp))
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            error?.let { message -> Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp)) }
            Text("${filtered.size} pelanggan", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth().clip(glassShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), glassShape), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)), shape = glassShape) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            Text(c.idpel, style = MaterialTheme.typography.labelLarge)
                            Text(c.address ?: "Alamat belum tersedia", style = MaterialTheme.typography.bodyMedium)
                            Text("${c.tariff ?: "-"} • ${c.powerVa ?: 0} VA • ${c.status ?: "-"}")
                            HorizontalDivider(Modifier.padding(vertical = 5.dp))
                            Text("Tagihan: Rp ${c.currentBill}")
                            Text("Tunggakan: Rp ${c.arrearsTotal}")
                            TextButton(onClick = { onShowMap(c) }, enabled = c.latitude != null && c.longitude != null) { Text(if (c.latitude != null && c.longitude != null) "Lihat Peta Pelanggan" else "Koordinat belum tersedia") }
                        }
                    }
                }
            }
        }
    }
}
