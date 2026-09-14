package id.bmax.app.feature.customer

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class CustomerDto(
    val id: String,
    val idpel: String,
    val name: String,
    val address: String? = null,
    @SerialName("no_meter") val meterNumber: String? = null,
    @SerialName("gardu_tiang") val garduTiang: String? = null,
    @SerialName("tariff_daya") val tariffPower: String? = null,
    val rbm: String? = null,
    @SerialName("wilker_biller") val billerName: String? = null,
    val status: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val billerId: String? = null,
    val currentBill: Double = 0.0,
    val arrearsTotal: Double = 0.0,
) {
    val tariff: String?
        get() = tariffPower?.substringBefore(" / ")?.takeIf { it.isNotBlank() }

    val powerVa: Int?
        get() = tariffPower?.substringAfter(" / ", "")?.trim()?.removeSuffix("VA")?.trim()?.toIntOrNull()
}

@Serializable
data class CurrentUserContextDto(
    @SerialName("user_id") val userId: String? = null,
    val email: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val role: String? = null,
    val active: Boolean = false,
    @SerialName("biller_id") val billerId: String? = null,
    @SerialName("biller_code") val billerCode: String? = null,
    @SerialName("biller_name") val billerName: String? = null,
    val username: String? = null,
)

class CustomerRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun getCurrentUserContext(): CurrentUserContextDto =
        supabase.postgrest.rpc("app_get_current_user_context")
            .decodeList<CurrentUserContextDto>()
            .firstOrNull()
            ?: throw IllegalStateException("Profil pengguna aktif tidak ditemukan di database.")

    /**
     * Reads the existing customer-info RPC. The RPC enforces database-side role/RLS scope,
     * therefore a BILLER cannot receive another BILLER's customers even if the UI filter changes.
     * BILLER scope is loaded page-by-page so the RBM filters can operate on the complete
     * customer set owned by the logged-in BILLER without using a global cache.
     */
    suspend fun getCustomers(): Pair<CurrentUserContextDto, List<CustomerDto>> {
        val context = getCurrentUserContext()
        val role = context.role.orEmpty().uppercase()
        val billerId = context.billerId?.trim().takeUnless { it.isNullOrEmpty() }
            ?: context.username?.trim().takeUnless { it.isNullOrEmpty() }

        val pageSize = 100
        val maxRows = if (role == "BILLER") 5_000 else 500
        val all = mutableListOf<CustomerDto>()
        var offset = 0

        while (all.size < maxRows) {
            val page = supabase.postgrest.rpc("app_list_customer_info") {
                parameter("p_query", "")
                parameter("p_limit", pageSize)
                parameter("p_offset", offset)
            }.decodeList<CustomerInfoRpcDto>()

            if (page.isEmpty()) break

            page.forEach { row ->
                val normalizedBiller = billerId?.trim()
                all += CustomerDto(
                    id = row.idpel,
                    idpel = row.idpel,
                    name = row.name,
                    address = row.address,
                    meterNumber = row.noMeter,
                    garduTiang = row.garduTiang,
                    tariffPower = row.tariffDaya,
                    rbm = row.rbm?.trim()?.takeIf { it.isNotEmpty() },
                    billerName = row.wilkerBiller,
                    status = row.status,
                    latitude = row.latitude,
                    longitude = row.longitude,
                    billerId = normalizedBiller,
                )
            }

            if (page.size < pageSize) break
            offset += page.size
        }

        return context to all.distinctBy { it.idpel }
    }

    /** Returns RBM options derived from the already scoped customer master. */
    fun getRbms(customers: List<CustomerDto>): List<String> =
        customers.mapNotNull { it.rbm?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .sorted()
}

@Serializable
private data class CustomerInfoRpcDto(
    val idpel: String,
    val name: String,
    val address: String? = null,
    @SerialName("no_meter") val noMeter: String? = null,
    @SerialName("gardu_tiang") val garduTiang: String? = null,
    @SerialName("tariff_daya") val tariffDaya: String? = null,
    val rbm: String? = null,
    @SerialName("wilker_biller") val wilkerBiller: String? = null,
    val status: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
