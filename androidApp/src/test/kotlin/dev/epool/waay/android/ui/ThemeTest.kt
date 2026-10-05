package dev.epool.waay.android.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import dev.epool.waay.android.ui.theme.WaayDarkColorScheme
import dev.epool.waay.android.ui.theme.WaayLightColorScheme
import dev.epool.waay.android.ui.theme.WaayTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/** Spec 002 FR-022: Material You (dynamic colour) on Android 12+, Wáay's generated scheme before. */
@RunWith(AndroidJUnit4::class)
class ThemeTest {
    @get:Rule
    val rule = createComposeRule()

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun schemeFor(dark: Boolean): ColorScheme {
        lateinit var scheme: ColorScheme
        rule.setContent { WaayTheme(darkTheme = dark) { scheme = MaterialTheme.colorScheme } }
        rule.waitForIdle()
        return scheme
    }

    @Test
    @Config(sdk = [30])
    fun withoutDynamicColourTheLightBrandSchemeIsUsed() {
        assertThat(schemeFor(dark = false)).isSameInstanceAs(WaayLightColorScheme)
    }

    @Test
    @Config(sdk = [30])
    fun withoutDynamicColourTheDarkBrandSchemeIsUsed() {
        assertThat(schemeFor(dark = true)).isSameInstanceAs(WaayDarkColorScheme)
    }

    @Test
    @Config(sdk = [36])
    fun withDynamicColourTheWallpaperSchemeIsUsed() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertThat(schemeFor(dark = false).primary).isEqualTo(dynamicLightColorScheme(context).primary)
    }

    @Test
    @Config(sdk = [36])
    fun withDynamicColourTheDarkWallpaperSchemeIsUsed() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertThat(schemeFor(dark = true).primary).isEqualTo(dynamicDarkColorScheme(context).primary)
    }
}
