package id.bmax.app.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository,
) : ViewModel() {
    private val _customers = MutableStateFlow<List<CustomerDto>>(emptyList())
    val customers: StateFlow<List<CustomerDto>> = _customers
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { repository.getCustomers() }
                .onSuccess { _customers.value = it }
                .onFailure { _error.value = it.message ?: "Gagal memuat pelanggan." }
            _loading.value = false
        }
    }
}
