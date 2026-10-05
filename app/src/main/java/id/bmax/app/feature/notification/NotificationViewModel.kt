package id.bmax.app.feature.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.postgres.PostgresAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class UserNotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val message: String,
    @SerialName("entity_type") val entityType: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<UserNotificationDto>>(emptyList())
    val notifications: StateFlow<List<UserNotificationDto>> = _notifications.asStateFlow()

    private val _realtimeConnected = MutableStateFlow(false)
    val realtimeConnected: StateFlow<Boolean> = _realtimeConnected.asStateFlow()

    private var listenJob: Job? = null
    private var statusJob: Job? = null
    private var channel: RealtimeChannel? = null
    private var activeUserId: String? = null

    fun start() {
        viewModelScope.launch {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
            if (activeUserId == userId && listenJob?.isActive == true) return@launch

            stop()
            activeUserId = userId
            loadLatest(userId)

            statusJob = viewModelScope.launch {
                supabase.realtime.status.collect { status ->
                    val value = status.toString().uppercase()
                    _realtimeConnected.value =
                        value.contains("CONNECTED") && !value.contains("RECONNECT")
                }
            }

            runCatching { supabase.realtime.connect() }
                .onFailure { Log.w(TAG, "Realtime connect failed", it) }

            val realtimeChannel = supabase.realtime.channel("bmax-user-notifications-$userId")
            channel = realtimeChannel

            val changeFlow = realtimeChannel
                .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "user_notifications"
                    filter = "user_id=eq.$userId"
                }

            listenJob = viewModelScope.launch {
                changeFlow
                    .catch { throwable ->
                        _realtimeConnected.value = false
                        Log.w(TAG, "Realtime notification stream failed", throwable)
                    }
                    .collectLatest { action ->
                        val item = action.decodeRecord<UserNotificationDto>()
                        if (item.userId != userId) return@collectLatest

                        _notifications.value = listOf(item) +
                            _notifications.value
                                .filterNot { it.id == item.id }
                                .take(49)
                    }
            }

            runCatching { realtimeChannel.subscribe() }
                .onFailure {
                    _realtimeConnected.value = false
                    Log.w(TAG, "Realtime notification subscribe failed", it)
                }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        statusJob?.cancel()
        statusJob = null
        channel?.let { existing ->
            viewModelScope.launch {
                runCatching { supabase.realtime.removeChannel(existing) }
            }
        }
        channel = null
        activeUserId = null
        _realtimeConnected.value = false
    }

    private suspend fun loadLatest(userId: String) {
        runCatching {
            supabase.from("user_notifications")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(50)
                }
                .decodeList<UserNotificationDto>()
        }.onSuccess { _notifications.value = it }
            .onFailure { Log.w(TAG, "Notification resync failed", it) }
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    companion object {
        private const val TAG = "BmaxRealtimeNotif"
    }
}
