package id.bmax.app.feature.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CustomerScreen(viewModel: CustomerViewModel, onLogout: () -> Unit) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    val filtered = if (search.isBlank()) customers else customers.filter { it.idpel.contains(search, true) || it.name.contains(search, true) }

    Scaffold(topBar = { TopAppBar(title = { Text("Pelanggan") }, actions = { TextButton(onClick = onLogout) { Text("Keluar") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(Modifier.fillMaxWidth(), search, { search = it }, label = { Text("Cari IDPEL / nama") }, singleLine = true)
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            Text(c.idpel, style = MaterialTheme.typography.labelLarge)
                            Text(c.address ?: "Alamat belum tersedia")
                            Text("${c.tariff ?: "-"} • ${c.powerVa ?: 0} VA • ${c.status ?: "-"}")
                            Text("Tagihan: Rp ${c.currentBill}")
                            Text("Tunggakan: Rp ${c.arrearsTotal}")
                        }
                    }
                }
            }
        }
    }
}
