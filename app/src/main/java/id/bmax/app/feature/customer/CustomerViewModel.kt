package id.bmax.app.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository,
) : ViewModel() {
    private val _customers = MutableStateFlow<List<CustomerDto>>(emptyList())
    val customers: StateFlow<List<CustomerDto>> = _customers.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _context = MutableStateFlow(CurrentUserContextDto())
    val context: StateFlow<CurrentUserContextDto> = _context.asStateFlow()

    private val _activeRbm = MutableStateFlow<String?>(null)
    val activeRbm: StateFlow<String?> = _activeRbm.asStateFlow()

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { repository.getCustomers() }
                .onSuccess { (userContext, rows) ->
                    _context.value = userContext
                    _customers.value = rows
                    _activeRbm.value = null
                }
                .onFailure { _error.value = it.message ?: "Gagal memuat data pelanggan dari Supabase." }
            _loading.value = false
        }
    }

    fun setSearch(value: String) {
        _search.value = value
    }

    fun setRbm(value: String?) {
        _activeRbm.value = value?.takeIf { it.isNotBlank() }
    }

    fun clearFilters() {
        _search.value = ""
        _activeRbm.value = null
    }

    fun filteredCustomers(): List<CustomerDto> {
        val query = _search.value.trim()
        val rbm = _activeRbm.value
        return _customers.value.filter { customer ->
            val textMatch = query.isBlank() ||
                customer.idpel.contains(query, ignoreCase = true) ||
                customer.name.contains(query, ignoreCase = true) ||
                customer.address.orEmpty().contains(query, ignoreCase = true) ||
                customer.rbm.orEmpty().contains(query, ignoreCase = true)
            val rbmMatch = rbm == null || customer.rbm.equals(rbm, ignoreCase = true)
            textMatch && rbmMatch
        }
    }

    fun rbmOptions(): List<String> = repository.getRbms(_customers.value)
}
