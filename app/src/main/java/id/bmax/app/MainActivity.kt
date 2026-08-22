package id.bmax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import id.bmax.app.core.ui.BmaxTheme
import id.bmax.app.feature.dashboard.DashboardScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BmaxTheme {
                DashboardScreen()
            }
        }
    }
}
