package dev.epool.waay.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.epool.waay.android.navigation.WaayNavDisplay
import dev.epool.waay.android.ui.theme.WaayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WaayTheme {
                WaayNavDisplay()
            }
        }
    }
}
