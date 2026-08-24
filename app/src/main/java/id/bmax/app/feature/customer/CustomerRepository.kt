package id.bmax.app.feature.customer

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class CustomerDto(
    val id: String,
    val idpel: String,
    val name: String,
    @SerialName("meter_number") val meterNumber: String? = null,
    val address: String? = null,
    val tariff: String? = null,
    @SerialName("power_va") val powerVa: Int? = null,
    val ulp: String? = null,
    val rbm: String? = null,
    val status: String? = null,
    @SerialName("current_bill") val currentBill: Double = 0.0,
    @SerialName("arrears_total") val arrearsTotal: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

class CustomerRepository @Inject constructor(private val supabase: SupabaseClient) {
    suspend fun getCustomers(): List<CustomerDto> = supabase.from("customers").select().decodeList<CustomerDto>()
}
