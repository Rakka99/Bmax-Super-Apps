package id.bmax.app.feature.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val repository: InvoiceRepository,
) : ViewModel() {
    private val _invoice = MutableStateFlow<InvoiceDisplayDto?>(null)
    val invoice: StateFlow<InvoiceDisplayDto?> = _invoice

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun load(idpel: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                repository.getInvoice(
                    idpel = idpel,
                    period = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM")),
                )
            }.onSuccess { data ->
                _invoice.value = data
                if (data == null) {
                    _error.value = "Data invoice periode berjalan belum tersedia."
                }
            }.onFailure {
                _error.value = it.message ?: "Gagal memuat data invoice."
            }
            _loading.value = false
        }
    }

    fun retry(idpel: String) = load(idpel)
}
