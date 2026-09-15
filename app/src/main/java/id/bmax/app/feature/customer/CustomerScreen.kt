package id.bmax.app.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: CustomerViewModel,
    onLogout: () -> Unit,
    onShowMap: (CustomerDto) -> Unit,
) {
    val context by viewModel.context.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val activeRbm by viewModel.activeRbm.collectAsStateWithLifecycle()

    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var draftRbm by rememberSaveable { mutableStateOf<String?>(null) }

    val filtered = remember(customers, search, activeRbm) {
        viewModel.filteredCustomers()
    }
    val rbmOptions = remember(customers) { viewModel.rbmOptions() }
    val role = context.role.orEmpty().uppercase()
    val currentBillerId = context.billerId?.takeIf { it.isNotBlank() } ?: context.username
    val currentBillerLabel = listOfNotNull(
        context.billerName?.takeIf { it.isNotBlank() },
        currentBillerId?.takeIf { it.isNotBlank() },
    ).joinToString(" • ").ifBlank { "Biller belum teridentifikasi" }

    val glassShape = RoundedCornerShape(22.dp)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("INFO PELANGGAN")
                        Text(
                            text = when {
                                activeRbm != null -> "RBM ${activeRbm} • ${filtered.size} data"
                                role == "BILLER" -> "Biller ${currentBillerId ?: "-"} • ${filtered.size} data"
                                else -> "Semua data sesuai hak akses • ${filtered.size} data"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        draftRbm = activeRbm
                        filterOpen = true
                    }) {
                        Icon(Icons.Default.FilterAlt, contentDescription = "Filter pelanggan")
                    }
                    TextButton(onClick = viewModel::refresh, enabled = !loading) { Text("Muat ulang") }
                    TextButton(onClick = onLogout) { Text("Keluar") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = viewModel::setSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text("Pencarian global: IDPEL / nama / alamat / RBM") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearFilters()
                    },
                    shape = RoundedCornerShape(16.dp),
                    enabled = search.isNotBlank() || activeRbm != null,
                ) {
                    Text("Semua Data")
                }
                OutlinedButton(
                    onClick = { filterOpen = true },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        when {
                            activeRbm != null -> activeRbm!!
                            role == "BILLER" -> "Biller Saya"
                            else -> "Biller / RBM"
                        },
                    )
                }
            }

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }

            error?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Text(
                text = "${filtered.size} pelanggan",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
            )

            if (!loading && filtered.isEmpty()) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = glassShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    ),
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Data pelanggan belum tampil", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (search.isNotBlank() || activeRbm != null) {
                                "Tidak ada pelanggan yang cocok dengan filter Biller/RBM/pencarian aktif."
                            } else {
                                "Periksa koneksi Supabase dan hak akses akun. Data Biller dibatasi oleh scope akun yang sedang login."
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
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clip(glassShape)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                glassShape,
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        ),
                        shape = glassShape,
                    ) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            Text(c.idpel, style = MaterialTheme.typography.labelLarge)
                            Text(c.address ?: "Alamat belum tersedia", style = MaterialTheme.typography.bodyMedium)
                            Text(c.garduTiang ?: "Gardu/Tiang: -")
                            Text(c.tariffPower ?: "Tarif/Daya: -")
                            Text("RBM: ${c.rbm ?: "-"}")
                            Text("WILKER / BILLER: ${c.billerName ?: currentBillerLabel}")
                            HorizontalRule()
                            Text("Tagihan: Rp ${c.currentBill}")
                            Text("Tunggakan: Rp ${c.arrearsTotal}")
                            TextButton(
                                onClick = { onShowMap(c) },
                                enabled = c.latitude != null && c.longitude != null,
                            ) {
                                Text(
                                    if (c.latitude != null && c.longitude != null) {
                                        "Lihat Peta Pelanggan"
                                    } else {
                                        "Koordinat belum tersedia"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (filterOpen) {
        ModalBottomSheet(onDismissRequest = { filterOpen = false }) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Filter Pencarian Pelanggan", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Scope database mengikuti akun yang sedang login.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedButton(
                    onClick = {
                        viewModel.clearFilters()
                        draftRbm = null
                        filterOpen = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("GLOBAL • SEMUA DATA PELANGGAN")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Biller", style = MaterialTheme.typography.labelLarge)
                        Text(currentBillerLabel, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (role == "BILLER") {
                                "Biller dikunci ke akun yang sedang login agar data Biller lain tidak pernah tampil."
                            } else {
                                "Admin/Supervisor mengikuti scope hak akses database."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Text("Pilih RBM", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { draftRbm = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Semua RBM Biller Saya")
                }

                rbmOptions.forEach { rbm ->
                    OutlinedButton(
                        onClick = { draftRbm = rbm },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(rbm)
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { filterOpen = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Batal") }
                    OutlinedButton(
                        onClick = {
                            viewModel.setRbm(draftRbm)
                            filterOpen = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Terapkan") }
                }
                Spacer(Modifier.padding(bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun HorizontalRule() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    )
}
