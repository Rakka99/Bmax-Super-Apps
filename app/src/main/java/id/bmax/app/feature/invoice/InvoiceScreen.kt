package id.bmax.app.feature.invoice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.bmax.app.feature.customer.CustomerDto
import java.text.NumberFormat
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    customer: CustomerDto,
    viewModel: InvoiceViewModel,
    onBack: () -> Unit,
) {
    val invoice by viewModel.invoice.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(customer.idpel) {
        viewModel.load(customer.idpel)
    }
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cetak Invoice")
                        Text("PLN Postpaid", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    ),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("PLN Electricity Services", style = MaterialTheme.typography.titleLarge)
                        Text("PLN UP3 SUMEDANG • Bmax Super Apps", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(customer.name, style = MaterialTheme.typography.titleMedium)
                        Text(customer.idpel, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (loading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            error?.let { message ->
                item {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(message, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = { viewModel.retry(customer.idpel) }) {
                                Text("Muat ulang")
                            }
                        }
                    }
                }
            }

            invoice?.let { data ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("INFORMASI TAGIHAN", style = MaterialTheme.typography.titleMedium)
                            InvoiceRow("ID Pelanggan", data.idpel)
                            InvoiceRow("Nama", data.customerName.ifBlank { customer.name })
                            InvoiceRow("Periode", data.period)
                            InvoiceRow("Kategori", data.category)
                            InvoiceRow("Status", if (data.status.equals("PAID", true)) "LUNAS" else "BELUM LUNAS")
                            HorizontalDivider()
                            InvoiceRow(
                                "Stand Meter",
                                data.standMeter?.trim()?.takeIf { it.isNotEmpty() } ?: "Belum tersedia",
                                monospace = true,
                            )
                        }
                    }
                }

                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("RINCIAN", style = MaterialTheme.typography.titleMedium)
                            InvoiceAmountRow("RPTAG PLN", data.rptagPln)
                            if (data.adminPln > 0) InvoiceAmountRow("Admin PLN", data.adminPln)
                            if (data.penalty > 0) InvoiceAmountRow("Denda", data.penalty)
                            HorizontalDivider()
                            InvoiceAmountRow("TOTAL", data.invoiceTotal, emphasize = true)
                        }
                    }
                }

                item {
                    Button(
                        onClick = { InvoicePrinter.print(context, customer, data) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cetak Invoice")
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String, monospace: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = if (monospace) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun InvoiceAmountRow(label: String, value: Long, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        Text(money(value), style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
    }
}

private fun money(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }.format(value)
