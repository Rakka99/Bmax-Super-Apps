package id.bmax.app.feature.customer

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class OperationalCustomerRow(
    @SerialName("server_id") val serverId: String,
    @SerialName("id_pelanggan") val idpel: String,
    @SerialName("nama") val name: String,
    @SerialName("alamat") val address: String? = null,
    @SerialName("tarif") val tariff: String? = null,
    @SerialName("daya") val powerVa: Int? = null,
    @SerialName("no_hp") val phone: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("biller_id") val billerId: String? = null,
    @SerialName("biller_name") val billerName: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("bill_amount") val billAmount: Long = 0,
    @SerialName("penalty_amount") val penaltyAmount: Long = 0,
    @SerialName("bill_period") val billPeriod: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("is_paid") val isPaid: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("rbm_code") val rbmCode: String? = null,
)

@Serializable
data class CustomerDto(
    val id: String,
    val idpel: String,
    val name: String,
    val meterNumber: String? = null,
    val address: String? = null,
    val tariff: String? = null,
    val powerVa: Int? = null,
    val ulp: String? = null,
    val rbm: String? = null,
    val status: String? = null,
    val currentBill: Double = 0.0,
    val arrearsTotal: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

class CustomerRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun getCustomers(): List<CustomerDto> {
        val result = mutableListOf<CustomerDto>()
        var offset = 0
        val pageSize = 100

        while (true) {
            val page = supabase.postgrest.rpc("search_operational_customers") {
                parameter("p_query", "")
                parameter("p_limit", pageSize)
                parameter("p_offset", offset)
            }.decodeList<OperationalCustomerRow>()

            if (page.isEmpty()) break

            result += page.map { row ->
                val total = row.billAmount.toDouble()
                CustomerDto(
                    id = row.serverId,
                    idpel = row.idpel,
                    name = row.name,
                    address = row.address,
                    tariff = row.tariff,
                    powerVa = row.powerVa,
                    ulp = row.billerName ?: row.billerId,
                    rbm = row.rbmCode,
                    status = row.status,
                    currentBill = total,
                    arrearsTotal = if (row.isPaid) 0.0 else total,
                    latitude = row.latitude,
                    longitude = row.longitude,
                )
            }

            if (page.size < pageSize) break
            offset += pageSize
        }

        return result
    }
}
