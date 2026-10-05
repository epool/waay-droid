package dev.epool.waay.android.ui

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the player asked for less motion (spec 002 FR-014, ADR-018). Android has no separate
 * reduce-motion switch: "Remove animations" in the accessibility settings sets the animator duration
 * scale to 0. Updated live, so a change made in the system settings applies on return.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduceMotion by remember(resolver) { mutableStateOf(resolver.animationsRemoved()) }
    DisposableEffect(resolver) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduceMotion = resolver.animationsRemoved()
                }
            }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduceMotion
}

/** True when "Remove animations" is on: the animator duration scale is 0. */
fun ContentResolver.animationsRemoved(): Boolean = Settings.Global.getFloat(this, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
