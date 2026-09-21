package id.bmax.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.bmax.app.core.ui.GlassCard
import id.bmax.app.feature.customer.CustomerViewModel
import java.text.NumberFormat
import java.util.Locale

private val PreventifColor = Color(0xFF73C58A)
private val KorektifColor = Color(0xFFF6D14A)
private val IrisanColor = Color(0xFFE8797A)

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    customerViewModel: CustomerViewModel,
    email: String?,
    role: String,
    onCustomers: () -> Unit,
    onLogout: () -> Unit,
) {
    val dashboard by dashboardViewModel.dashboard.collectAsStateWithLifecycle()
    val categories by dashboardViewModel.categories.collectAsStateWithLifecycle()
    val period by dashboardViewModel.period.collectAsStateWithLifecycle()
    val loading by dashboardViewModel.loading.collectAsStateWithLifecycle()
    val error by dashboardViewModel.error.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("SEMUA") }
    LaunchedEffect(Unit) { dashboardViewModel.refresh() }

    val totalOpenTasks = categories.sumOf { it.unpaid_count }.coerceAtLeast(0)
    val visibleOpenTasks = when (selectedCategory) {
        "PREVENTIF", "KOREKTIF", "IRISAN" ->
            categories.firstOrNull { it.category.uppercase() == selectedCategory }?.unpaid_count ?: 0
        else -> totalOpenTasks
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFEAF4FF), Color(0xFFF7FAFF))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfileCard(email = email, role = role, period = period)

            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "SALDO RPTAG PLN",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            money(dashboard.unpaid_amount),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Periode ${periodLabel(period)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tanpa admin dan tanpa denda • ${dashboard.unpaid_customers} tagihan UNPAID",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Saldo Berjalan") }
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Saldo = RP TAG PLN saja • admin dan denda tidak dihitung",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "TOTAL PELANGGAN SAYA",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    listOf(
                        Triple("Total", dashboard.total_customers, "Tagihan"),
                        Triple("Sudah Bayar", dashboard.paid_customers, "Lunas"),
                        Triple("Belum Bayar", dashboard.unpaid_customers, "Perlu Pantau")
                    )
                ) { (label, value, note) ->
                    SummaryCard(
                        modifier = Modifier.width(150.dp),
                        label = label,
                        value = value.toString(),
                        note = note,
                        valueColor = when (label) {
                            "Sudah Bayar" -> Color(0xFF238B57)
                            "Belum Bayar" -> Color(0xFFD83B3B)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "RBM (Report Business Management)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Distribusi & Drill-Down • hanya tugas yang masih UNPAID",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(14.dp))

                categories.forEach { category ->
                    CategoryProgressRow(category)
                    Spacer(Modifier.height(10.dp))
                }

                val totalForBar = categories.sumOf { it.total_count }.coerceAtLeast(1)
                val openRatio = categories.sumOf { it.unpaid_count }.toFloat() / totalForBar.toFloat()

                Text(
                    "Proporsi Tugas Aktif",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { openRatio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${totalOpenTasks} tugas aktif • ${dashboard.collection_rate.toInt()}% tertagih",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        CategoryMiniCard(category = category, modifier = Modifier.weight(1f))
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "Detail Penugasan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Daftar tugas aktif hanya berasal dari billing berstatus UNPAID. Billing PAID/Lunas dan shared RBM Supervisor tidak ditampilkan sebagai tugas aktif Biller.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf(
                        "SEMUA" to "Semua (${totalOpenTasks})",
                        "PREVENTIF" to "Preventif (${categories.countOpen("PREVENTIF")})",
                        "KOREKTIF" to "Korektif (${categories.countOpen("KOREKTIF")})",
                        "IRISAN" to "Irisan (${categories.countOpen("IRISAN")})"
                    )
                    items(chips) { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    tonalElevation = 1.dp
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            if (selectedCategory == "SEMUA")
                                "Menampilkan ${visibleOpenTasks} tugas UNPAID"
                            else
                                "Menampilkan ${visibleOpenTasks} tugas UNPAID • ${selectedCategory.lowercase().replaceFirstChar { it.uppercase() }}",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Status PAID/Lunas tersimpan sebagai histori. Saldo di atas hanya RP TAG PLN dari UNPAID, tanpa admin dan tanpa denda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCustomers,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("Pelanggan") }

                OutlinedButton(
                    onClick = { dashboardViewModel.refresh() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) { Text(if (loading) "Memuat..." else "Refresh") }
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Keluar Akun") }

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(email: String?, role: String, period: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Bmax Super Apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(email ?: "Pengguna Bmax", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "● Online • Sync aktif • ${periodLabel(period)}",
                    color = Color(0xFF238B57),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            AssistChip(onClick = {}, enabled = false, label = { Text(role.uppercase()) })
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    label: String,
    value: String,
    note: String,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(note, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CategoryProgressRow(category: CategoryStatusDto) {
    val accent = category.color()
    val total = category.total_count.coerceAtLeast(0)
    val open = category.unpaid_count.coerceAtLeast(0)
    val paidPct = if (total > 0) (category.paid_count * 100 / total).toInt() else 0

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(10.dp)
                .height(10.dp)
                .background(accent, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(category.label(), fontWeight = FontWeight.SemiBold)
            Text(
                if (open == 0L) "0 tugas aktif • semua lunas" else "$open tugas aktif",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("$open ($paidPct% Lunas)", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryMiniCard(
    category: CategoryStatusDto,
    modifier: Modifier
) {
    val accent = category.color()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.14f),
        tonalElevation = 1.dp
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(category.label(), fontWeight = FontWeight.SemiBold)
            Text(
                category.unpaid_count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (category.unpaid_count == 0L) "0 tugas aktif" else "UNPAID",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun CategoryStatusDto.label(): String =
    when (category.uppercase()) {
        "PREVENTIF" -> "Preventif"
        "KOREKTIF" -> "Korektif"
        "IRISAN" -> "Irisan"
        else -> category
    }

private fun CategoryStatusDto.color(): Color =
    when (category.uppercase()) {
        "PREVENTIF" -> PreventifColor
        "KOREKTIF" -> KorektifColor
        "IRISAN" -> IrisanColor
        else -> Color(0xFF2F80ED)
    }

private fun List<CategoryStatusDto>.countOpen(category: String): Long =
    firstOrNull { it.category.equals(category, ignoreCase = true) }?.unpaid_count ?: 0L

private fun money(value: Double): String =
    "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(value)

private fun periodLabel(period: String): String {
    val month = period.takeIf { it.length == 6 }?.substring(4, 6)?.toIntOrNull() ?: return period
    val year = period.takeIf { it.length == 6 }?.substring(0, 4) ?: return period
    val names = listOf(
        "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    return "${names.getOrElse(month) { "" }} $year".trim()
}
