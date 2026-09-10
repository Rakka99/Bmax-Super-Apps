package id.bmax.app.feature.ddtn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DdtnPlnViewModel @Inject constructor(
    private val repository: DdtnPlnRepository,
) : ViewModel() {
    private val _results = MutableStateFlow<Map<String, DdtnCustomerResult>>(emptyMap())
    val results: StateFlow<Map<String, DdtnCustomerResult>> = _results.asStateFlow()

    private val _busy = MutableStateFlow<Set<String>>(emptySet())
    val busy: StateFlow<Set<String>> = _busy.asStateFlow()

    fun check(idpel: String) {
        if (idpel in _busy.value) return
        viewModelScope.launch {
            _busy.value = _busy.value + idpel
            runCatching { repository.checkCustomer(idpel) }
                .onSuccess { result ->
                    _results.value = _results.value + (idpel to result)
                }
                .onFailure { error ->
                    _results.value = _results.value + (
                        idpel to DdtnCustomerResult(
                            idpel = idpel,
                            status = DdtnDisplayStatus.ERROR,
                            detail = error.message ?: "Pemeriksaan gagal.",
                        )
                    )
                }
            _busy.value = _busy.value - idpel
        }
    }
}
