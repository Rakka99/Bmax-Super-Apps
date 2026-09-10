package id.bmax.app.feature.ddtn

import id.bmax.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@Serializable
data class DdtnSyncResponse(
    val success: Boolean = false,
    val request_id: String? = null,
    val updated: List<DdtnUpdated> = emptyList(),
    val unverified: List<DdtnUnverified> = emptyList(),
    val unmatched: List<DdtnUnmatched> = emptyList(),
    val errors: List<DdtnError> = emptyList(),
    val error: String? = null,
    val provider_http_status: Int? = null,
    val retryable: Boolean? = null,
)

@Serializable
data class DdtnUpdated(
    val customer_no: String,
    val period: String,
    val old_status: String? = null,
    val new_status: String,
    val total_due: Double? = null,
    val local_total_amount: Double? = null,
    val amount_difference: Double? = null,
    val provider_rc: String? = null,
    val reason: String? = null,
    val checked_at: String? = null,
)

@Serializable
data class DdtnUnverified(
    val customer_no: String? = null,
    val period: String? = null,
    val is_paid: Boolean? = null,
    val reason: String? = null,
    val provider_rc: String? = null,
)

@Serializable
data class DdtnUnmatched(
    val customer_no: String? = null,
    val period: String? = null,
    val reason: String? = null,
)

@Serializable
data class DdtnError(
    val customer_no: String? = null,
    val period: String? = null,
    val error: String? = null,
)

enum class DdtnDisplayStatus {
    LUNAS,
    BELUM_LUNAS,
    BELUM_TERVERIFIKASI,
    ERROR,
}

data class DdtnCustomerResult(
    val idpel: String,
    val status: DdtnDisplayStatus,
    val period: String? = null,
    val detail: String? = null,
)

class DdtnPlnRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkCustomer(idpel: String): DdtnCustomerResult {
        val token = supabase.auth.currentAccessTokenOrNull()
            ?: throw IllegalStateException("Sesi login Supabase tidak tersedia. Silakan login kembali.")

        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/ddtn-pln-sync"
        val payload = buildJsonObject {
            putJsonArray("customer_nos") { add(idpel) }
        }.toString()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Accept", "application/json")
            .post(payload.toRequestBody(mediaType))
            .build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val body = runCatching { json.decodeFromString<DdtnSyncResponse>(raw) }.getOrNull()

            if (!response.isSuccessful || body?.success != true) {
                val provider = body?.provider_http_status
                val message = when {
                    provider == 429 -> "Quota/inquiry sedang dibatasi. Coba lagi beberapa saat."
                    provider in listOf(500, 502, 503, 504) -> "DDTN sedang tidak dapat memproses inquiry. Coba lagi beberapa saat."
                    response.code == 401 -> "Sesi login tidak valid. Silakan login kembali."
                    response.code == 403 -> "Anda tidak memiliki akses untuk memeriksa pelanggan ini."
                    else -> body?.error ?: "Pemeriksaan status PLN gagal (HTTP ${response.code})."
                }
                throw IllegalStateException(message)
            }

            body.updated.firstOrNull()?.let { updated ->
                return DdtnCustomerResult(
                    idpel = updated.customer_no,
                    status = if (updated.new_status == "PAID") DdtnDisplayStatus.LUNAS else DdtnDisplayStatus.BELUM_LUNAS,
                    period = updated.period,
                    detail = updated.reason,
                )
            }

            body.unverified.firstOrNull()?.let { item ->
                return DdtnCustomerResult(
                    idpel = item.customer_no ?: idpel,
                    status = DdtnDisplayStatus.BELUM_TERVERIFIKASI,
                    period = item.period,
                    detail = item.reason ?: "Status belum dapat diverifikasi dari provider.",
                )
            }

            body.unmatched.firstOrNull()?.let { item ->
                throw IllegalStateException(item.reason ?: "Data billing pelanggan tidak ditemukan di Bmax.")
            }

            body.errors.firstOrNull()?.let { item ->
                throw IllegalStateException(item.error ?: "Gagal memperbarui billing pelanggan.")
            }

            throw IllegalStateException("DDTN tidak mengembalikan hasil pemeriksaan untuk $idpel.")
        }
    }
}
