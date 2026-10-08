package id.bmax.app.feature.invoice

import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class InvoiceDisplayDto(
    val id: String,
    val idpel: String,
    @SerialName("customer_name") val customerName: String = "",
    val period: String = "",
    val status: String = "UNPAID",
    val category: String = "PREVENTIF",
    @SerialName("rptag_pln") val rptagPln: Long = 0,
    @SerialName("admin_pln") val adminPln: Long = 0,
    val penalty: Long = 0,
    @SerialName("invoice_total") val invoiceTotal: Long = 0,
    @SerialName("rbm_code") val rbmCode: String? = null,
    @SerialName("biller_id") val billerId: String? = null,
    @SerialName("stand_meter") val standMeter: String? = null,
    @SerialName("billing_period") val billingPeriod: String? = null,
)

class InvoiceRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun getInvoice(idpel: String, period: String): InvoiceDisplayDto? {
        return supabase.postgrest
            .from("invoice_billing_display")
            .select()
            .eq("idpel", idpel)
            .eq("billing_period", period)
            .limit(1)
            .decodeList<InvoiceDisplayDto>()
            .firstOrNull()
    }
}
