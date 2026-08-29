package id.bmax.app.feature.dashboard

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class BillingDashboardDto(
    val total_customers: Long = 0,
    val total_bills: Long = 0,
    val unpaid_bills: Long = 0,
    val paid_bills: Long = 0,
    val total_billed: Double = 0.0,
    val total_paid: Double = 0.0,
    val total_unpaid: Double = 0.0,
    val collection_rate: Double = 0.0,
)

class DashboardRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun getDashboard(period: String? = null): BillingDashboardDto =
        supabase.postgrest.rpc("get_billing_dashboard") {
            parameter("p_period", period)
        }.decodeSingle()
}
