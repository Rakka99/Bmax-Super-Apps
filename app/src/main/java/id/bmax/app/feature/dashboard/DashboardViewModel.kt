package id.bmax.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
) : ViewModel() {
    private val _dashboard = MutableStateFlow(BillingDashboardDto())
    val dashboard: StateFlow<BillingDashboardDto> = _dashboard.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh(period: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { repository.getDashboard(period) }
                .onSuccess { _dashboard.value = it }
                .onFailure { _error.value = it.message ?: "Gagal memuat dashboard" }
            _loading.value = false
        }
    }
}
