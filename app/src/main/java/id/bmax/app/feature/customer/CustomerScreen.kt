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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.bmax.app.feature.ddtn.DdtnDisplayStatus
import id.bmax.app.feature.ddtn.DdtnPlnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(viewModel: CustomerViewModel, onLogout: () -> Unit, onShowMap: (CustomerDto) -> Unit) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val ddtnViewModel: DdtnPlnViewModel = hiltViewModel()
    val ddtnResults by ddtnViewModel.results.collectAsStateWithLifecycle()
    val ddtnBusy by ddtnViewModel.busy.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    val filtered = if (search.isBlank()) customers else customers.filter {
        it.idpel.contains(search, true) || it.name.contains(search, true)
    }
    val glassShape = RoundedCornerShape(22.dp)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Column { Text("Bmax Super Apps"); Text("Pelanggan", style = MaterialTheme.typography.labelMedium) } },
                actions = {
                    TextButton(onClick = viewModel::refresh, enabled = !loading) { Text("Muat ulang") }
                    TextButton(onClick = onLogout) { Text("Keluar") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Cari IDPEL / nama") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
            }
            Text("${filtered.size} pelanggan", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))

            if (!loading && filtered.isEmpty()) {
                Card(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = glassShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Data pelanggan belum tampil", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (search.isNotBlank()) {
                                "Tidak ada pelanggan yang cocok dengan pencarian."
                            } else {
                                "Periksa koneksi Supabase dan hak akses akun. Admin/Supervisor dapat melihat seluruh data pelanggan; Biller hanya melihat pelanggan yang ditugaskan."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = viewModel::refresh) { Text("Coba lagi") }
                    }
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.id }) { c ->
                    val ddtnResult = ddtnResults[c.idpel]
                    val isChecking = c.idpel in ddtnBusy
                    Card(
                        Modifier.fillMaxWidth().clip(glassShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), glassShape),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
                        shape = glassShape,
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            Text(c.idpel, style = MaterialTheme.typography.labelLarge)
                            Text(c.address ?: "Alamat belum tersedia", style = MaterialTheme.typography.bodyMedium)
                            Text("${c.tariff ?: "-"} • ${c.powerVa ?: 0} VA • ${c.status ?: "-"}")
                            HorizontalDivider(Modifier.padding(vertical = 5.dp))
                            Text("Tagihan: Rp ${c.currentBill}")
                            Text("Tunggakan: Rp ${c.arrearsTotal}")

                            ddtnResult?.let { result ->
                                val label = when (result.status) {
                                    DdtnDisplayStatus.LUNAS -> "DDTN: LUNAS"
                                    DdtnDisplayStatus.BELUM_LUNAS -> "DDTN: BELUM LUNAS"
                                    DdtnDisplayStatus.BELUM_TERVERIFIKASI -> "DDTN: BELUM TERVERIFIKASI"
                                    DdtnDisplayStatus.ERROR -> "DDTN: PEMERIKSAAN GAGAL"
                                }
                                val supportingText = buildString {
                                    result.period?.let { append("Periode $it") }
                                    result.detail?.let {
                                        if (isNotEmpty()) append(" • ")
                                        append(it)
                                    }
                                }
                                AssistChip(onClick = {}, label = { Text(label) })
                                if (supportingText.isNotBlank()) {
                                    Text(supportingText, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { ddtnViewModel.check(c.idpel) },
                                    enabled = !isChecking,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text(if (isChecking) "Memeriksa..." else "Cek PLN DDTN")
                                }
                                TextButton(onClick = { onShowMap(c) }, enabled = c.latitude != null && c.longitude != null) {
                                    Text(if (c.latitude != null && c.longitude != null) "Peta" else "No GPS")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
