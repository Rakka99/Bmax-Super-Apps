package id.bmax.app.feature.customer

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class CustomerDto(
    val id: String,
    @SerialName("id_pelanggan") val idpel: String,
    @SerialName("nama") val name: String,
    @SerialName("alamat") val address: String? = null,
    @SerialName("tarif") val tariffValue: String? = null,
    @SerialName("daya") val powerValue: Int? = null,
    @SerialName("rbm_code") val rbm: String? = null,
    @SerialName("status") val statusValue: String? = null,
    @SerialName("biller_id") val billerId: String? = null,
    val currentBill: Double = 0.0,
    val arrearsTotal: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val meterNumber: String? get() = null
    val garduTiang: String? get() = null
    val billerName: String? get() = billerId
    val status: String? get() = statusValue
    val tariff: String? get() = tariffValue
    val powerVa: Int? get() = powerValue
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
     * Reads the existing customers table directly and scopes BILLER data before it enters UI state.
     * Pagination prevents the customer screen from treating a single page as the total dataset.
     */
    suspend fun getCustomers(): Pair<CurrentUserContextDto, List<CustomerDto>> {
        val context = getCurrentUserContext()
        val role = context.role.orEmpty().uppercase()
        val billerId = context.billerId?.trim().takeUnless { it.isNullOrEmpty() }
            ?: context.username?.trim().takeUnless { it.isNullOrEmpty() }

        if (role == "BILLER" && billerId.isNullOrBlank()) {
            throw IllegalStateException("Biller ID pengguna aktif tidak ditemukan; data customer tidak dimuat untuk mencegah data lintas Biller.")
        }

        val pageSize = 100
        val maxPages = 50
        val all = mutableListOf<CustomerDto>()
        var offset = 0

        repeat(maxPages) {
            val page = supabase.from("customers").select {
                if (role == "BILLER") {
                    filter { eq("biller_id", billerId!!) }
                }
                order("nama", Order.ASCENDING)
                order("id_pelanggan", Order.ASCENDING)
                range(offset.toLong(), (offset + pageSize - 1).toLong())
            }.decodeList<CustomerRow>()

            if (page.isEmpty()) return@repeat

            all += page.map { it.toDto(billerId) }
            if (page.size < pageSize) return@repeat
            offset += page.size
        }

        return context to all.distinctBy { it.idpel }
    }

    fun getRbms(customers: List<CustomerDto>): List<String> =
        customers.mapNotNull { it.rbm?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .sorted()
}

@Serializable
private data class CustomerRow(
    val id: String,
    @SerialName("id_pelanggan") val idpel: String,
    @SerialName("nama") val name: String,
    @SerialName("alamat") val address: String? = null,
    @SerialName("tarif") val tariff: String? = null,
    @SerialName("daya") val powerVa: Int? = null,
    @SerialName("rbm_code") val rbm: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("biller_id") val billerId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    fun toDto(fallbackBillerId: String?): CustomerDto = CustomerDto(
        id = id,
        idpel = idpel,
        name = name,
        address = address,
        tariffValue = tariff,
        powerValue = powerVa,
        rbm = rbm,
        statusValue = status,
        billerId = billerId ?: fallbackBillerId,
        latitude = latitude,
        longitude = longitude,
    )
}
