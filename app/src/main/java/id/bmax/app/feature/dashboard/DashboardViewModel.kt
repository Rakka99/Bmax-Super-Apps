package id.bmax.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
) : ViewModel() {
    private val _dashboard = MutableStateFlow(OperationalDashboardDto())
    val dashboard: StateFlow<OperationalDashboardDto> = _dashboard.asStateFlow()

    private val _categories = MutableStateFlow(
        listOf(
            CategoryStatusDto(category = "PREVENTIF"),
            CategoryStatusDto(category = "KOREKTIF"),
            CategoryStatusDto(category = "IRISAN"),
        )
    )
    val categories: StateFlow<List<CategoryStatusDto>> = _categories.asStateFlow()

    private val _period = MutableStateFlow(currentPeriod())
    val period: StateFlow<String> = _period.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            runCatching {
                val activePeriod = currentPeriod()
                val dashboard = repository.getOperationalDashboard()
                val rawCategories = repository.getCategoryStatus(activePeriod)
                val byCategory = rawCategories.associateBy { it.category.uppercase() }
                val normalized = listOf("PREVENTIF", "KOREKTIF", "IRISAN").map { category ->
                    byCategory[category] ?: CategoryStatusDto(category = category)
                }
                Triple(activePeriod, dashboard, normalized)
            }.onSuccess { (activePeriod, dashboard, normalized) ->
                _period.value = activePeriod
                _dashboard.value = dashboard
                _categories.value = normalized
            }.onFailure {
                _error.value = it.message ?: "Gagal memuat dashboard"
            }

            _loading.value = false
        }
    }

    private fun currentPeriod(): String =
        YearMonth.now(ZoneId.of("Asia/Jakarta")).toString().replace("-", "")
}
