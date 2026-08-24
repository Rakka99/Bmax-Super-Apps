package id.bmax.app.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun CustomerMapScreen(customerName: String, latitude: Double?, longitude: Double?, onBack: () -> Unit) {
    val position = if (latitude != null && longitude != null) LatLng(latitude, longitude) else LatLng(-6.914744, 107.609810)
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(position, 15f) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFFEAF4FF), androidx.compose.ui.graphics.Color(0xFFD8F4FF))))) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = MapProperties(isBuildingEnabled = true, isTrafficEnabled = false),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
        ) { if (latitude != null && longitude != null) Marker(position = position, title = customerName, snippet = "Lokasi pelanggan") }
        Surface(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(22.dp)), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), tonalElevation = 3.dp) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(customerName, style = MaterialTheme.typography.titleMedium)
                    Text(if (latitude != null && longitude != null) "${"%.6f".format(latitude)}, ${"%.6f".format(longitude)}" else "Koordinat belum tersedia")
                }
                TextButton(onClick = onBack) { Text("Kembali") }
            }
        }
    }
}
