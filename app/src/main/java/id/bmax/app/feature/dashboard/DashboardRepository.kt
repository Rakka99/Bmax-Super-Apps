package id.bmax.app.feature.dashboard

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class OperationalDashboardDto(
    val total_customers: Long = 0,
    val paid_customers: Long = 0,
    val unpaid_customers: Long = 0,
    val total_amount: Double = 0.0,
    val paid_amount: Double = 0.0,
    val unpaid_amount: Double = 0.0,
    val collection_rate: Double = 0.0,
    val preventif_count: Long = 0,
    val preventif_amount: Double = 0.0,
    val preventif_paid: Double = 0.0,
    val korektif_count: Long = 0,
    val korektif_amount: Double = 0.0,
    val korektif_paid: Double = 0.0,
    val irisan_count: Long = 0,
    val irisan_amount: Double = 0.0,
    val irisan_paid: Double = 0.0,
    val visit_count: Long = 0,
    val pdil_count: Long = 0,
    val pending_pdil_count: Long = 0,
)

@Serializable
data class CategoryStatusDto(
    val category: String = "",
    val total_count: Long = 0,
    val paid_count: Long = 0,
    val unpaid_count: Long = 0,
    val total_amount: Double = 0.0,
    val paid_amount: Double = 0.0,
    val unpaid_amount: Double = 0.0,
)

class DashboardRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun getOperationalDashboard(): OperationalDashboardDto =
        supabase.postgrest.rpc("get_operational_dashboard_stats").decodeSingle()

    suspend fun getCategoryStatus(period: String): List<CategoryStatusDto> =
        supabase.postgrest.rpc("get_operational_category_status") {
            parameter("p_period", period)
        }.decodeList()
}
